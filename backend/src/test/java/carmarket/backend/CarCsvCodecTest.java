package carmarket.backend;
import carmarket.backend.service.CarCsvCodec;
import carmarket.backend.dto.*;
import carmarket.backend.domain.CarStatus;
import carmarket.backend.error.ApiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
class CarCsvCodecTest {
    private final CarCsvCodec codec=new CarCsvCodec(new ObjectMapper());
    private CarResponse car(String code,String description,Set<String> tags) {
        var ref=new CatalogRef(1,"x",true);
        return new CarResponse(UUID.randomUUID(),code,new SellerResponse(1,"x"),ref,ref,2020,100,new BigDecimal("123.45"),null,
            ref,ref,ref,new BigDecimal("2.0"),description,CarStatus.AVAILABLE,true,tags,10,false,0,Instant.now(),Instant.now());
    }
    @Test void roundTripPreservesUnicodeCommasQuotesNewlinesAndTagSeparators() {
        var c=car("Код, \"А\"","Первая строка\r\nВторая, \"текст\"",Set.of("a|b","кавычки \"x\"","обратная \\"));
        var row=codec.parse("\uFEFF"+codec.export(List.of(c))).getFirst();
        assertThat(row.error()).isNull();assertThat(row.data().externalCode()).isEqualTo(c.externalCode());
        assertThat(row.data().description()).isEqualTo(c.description());assertThat(row.data().tags()).isEqualTo(c.tags());
    }
    @Test void formulaProtectionIsReversibleAndPreservesApostrophes() {
        for(String value:List.of("=SUM(1,2)","  +1","-12","@foo","'literal","\t=1","\n=1")) {
            String source=codec.export(List.of(car(value,value,Set.of())));
            assertThat(source).contains("'"+value);
            var row=codec.parse(source).getFirst();
            assertThat(row.data().externalCode()).isEqualTo(value);assertThat(row.data().description()).isEqualTo(value);
        }
    }
    @Test void malformedCsvAndHeadersAreRejected() {
        assertThatThrownBy(()->codec.parse("externalCode,year\r\nx,2020")).isInstanceOf(ApiFailure.class);
        assertThatThrownBy(()->codec.parse(String.join(",",CarCsvCodec.HEADER)+"\r\n\"unterminated")).isInstanceOf(ApiFailure.class);
        assertThatThrownBy(()->codec.parse(String.join(",",CarCsvCodec.HEADER)+"\r\n")).isInstanceOf(ApiFailure.class);
    }
    @Test void malformedRowReturnsItsRecordNumberAndError() {
        var rows=codec.parse(String.join(",",CarCsvCodec.HEADER)+"\r\nx,wrong\r\n");
        assertThat(rows.getFirst().line()).isEqualTo(2);assertThat(rows.getFirst().error()).isEqualTo("INVALID_ROW");
    }
}
