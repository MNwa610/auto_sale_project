package carmarket.backend.service;
import carmarket.backend.domain.CarStatus;
import carmarket.backend.dto.*;
import carmarket.backend.error.ApiFailure;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

@Component
public class CarCsvCodec {
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    public CarCsvCodec(com.fasterxml.jackson.databind.ObjectMapper json) { this.json=json; }
    public static final String[] HEADER={"externalCode","sellerId","modelId","year","mileage","price","vin","bodyTypeId",
        "transmissionId","fuelTypeId","engineVolume","description","status","certified","tags"};
    public record Row(int line,CarWriteRequest data,String error) { }
    public List<Row> parse(String source) {
        if(source.startsWith("\uFEFF"))source=source.substring(1);
        List<Row> rows=new ArrayList<>();
        try(var parser=CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(new StringReader(source))) {
            if(!parser.getHeaderNames().equals(List.of(HEADER))) throw ApiFailure.invalid("Неверный заголовок CSV. Используйте формат экспорта.");
            for(var r:parser) {
                if(rows.size()>=2000)throw ApiFailure.invalid("Импорт ограничен 2000 строк.");
                int line=(int)r.getRecordNumber()+1;
                try {
                    if(r.size()!=HEADER.length)throw new IllegalArgumentException();
                    String bool=r.get("certified"); if(!bool.equals("true")&&!bool.equals("false"))throw new IllegalArgumentException();
                    Set<String> tags=new LinkedHashSet<>();
                    if(!r.get("tags").isBlank()) {
                        try {
                            String[] values=json.readValue(r.get("tags"),String[].class);
                            if(values==null)throw new IllegalArgumentException();
                            tags.addAll(Arrays.asList(values));
                        } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalArgumentException(); }
                    }
                    rows.add(new Row(line,new CarWriteRequest(unsafe(r.get("externalCode")),Long.valueOf(r.get("sellerId")),Long.valueOf(r.get("modelId")),
                        Integer.valueOf(r.get("year")),Integer.valueOf(r.get("mileage")),new BigDecimal(r.get("price")),
                        r.get("vin"),Long.valueOf(r.get("bodyTypeId")),Long.valueOf(r.get("transmissionId")),Long.valueOf(r.get("fuelTypeId")),
                        new BigDecimal(r.get("engineVolume")),unsafe(r.get("description")),CarStatus.valueOf(r.get("status")),Boolean.valueOf(bool),tags,null),null));
                } catch(IllegalArgumentException e) { rows.add(new Row(line,null,"INVALID_ROW")); }
            }
        } catch(IOException|UncheckedIOException e) { throw ApiFailure.invalid("Некорректная структура CSV."); }
        if(rows.isEmpty())throw ApiFailure.invalid("CSV не содержит данных.");
        return rows;
    }
    public String export(List<CarResponse> cars) {
        var out=new StringWriter();
        try(var printer=new CSVPrinter(out,CSVFormat.RFC4180.builder().setHeader(HEADER).get())) {
            for(var c:cars) {
                // Префикс Excel не меняет обычные значения. Потребитель видит текст, а не формулу.
                printer.printRecord(safe(c.externalCode()),c.seller().id(),c.model().id(),c.year(),c.mileage(),c.price(),c.vin(),
                    c.bodyType()==null?null:c.bodyType().id(),c.transmission()==null?null:c.transmission().id(),c.fuelType()==null?null:c.fuelType().id(),
                    c.engineVolume(),safe(c.description()),c.status(),c.certified(),json.writeValueAsString(c.tags()));
            }
        }catch(IOException e){throw new IllegalStateException(e);}
        return out.toString();
    }
    private String unsafe(String value) {
        if(value.startsWith("'") && value.length()>1 && safe(value.substring(1)).equals(value)) return value.substring(1);
        return value;
    }
    private String safe(String value) {
        if(value==null||value.isEmpty())return value;
        char first=value.stripLeading().isEmpty()?' ':value.stripLeading().charAt(0);
        return "=+-@".indexOf(first)>=0 || value.charAt(0)=='\'' || value.charAt(0)=='\t' || value.charAt(0)=='\r' ? "'"+value:value;
    }
}
