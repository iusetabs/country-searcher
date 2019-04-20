package country.searcher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Configuration
@EnableAsync
@Controller
@RestController
public class CountriesController {
    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    public CountriesController(){
    }

    @RequestMapping(path="/countries", method=POST)
    public @ResponseBody
    ArrayList<Country> getNearestCountry(@RequestBody Country c) throws ExecutionException, InterruptedException {
        Future<ArrayList<Country>> future = asyncGetNearestCountry(c);
        while (true) {
            if (future.isDone()) {
                return future.get();
            }
            Thread.sleep(10);
        }
    }
    @Async("threadPoolTaskExecutor")
    public Future<ArrayList<Country>> asyncGetNearestCountry(Country c){
       ArrayList<Country> al = new ArrayList<>();
       al.add(c);
       return new AsyncResult<>(al); //Placeholder
    }


}
