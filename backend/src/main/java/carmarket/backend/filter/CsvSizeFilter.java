package carmarket.backend.filter;
import carmarket.backend.security.SecurityProblemWriter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
@Component
public class CsvSizeFilter extends OncePerRequestFilter {
    private final SecurityProblemWriter problems;
    public CsvSizeFilter(SecurityProblemWriter problems){this.problems=problems;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        if(!request.getMethod().equals("POST")||!request.getRequestURI().equals("/api/v1/cars/csv/preview")){chain.doFilter(request,response);return;}
        byte[] body=request.getInputStream().readNBytes(2*1024*1024+1);
        if(body.length>2*1024*1024){problems.write(request,response,HttpStatus.PAYLOAD_TOO_LARGE,"CSV_TOO_LARGE","CSV ограничен 2 MiB.");return;}
        chain.doFilter(new HttpServletRequestWrapper(request){
            public ServletInputStream getInputStream(){
                var input=new ByteArrayInputStream(body);
                return new ServletInputStream(){
                    public int read(){return input.read();}
                    public boolean isFinished(){return input.available()==0;}
                    public boolean isReady(){return true;}
                    public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                };
            }
            public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
        },response);
    }
}
