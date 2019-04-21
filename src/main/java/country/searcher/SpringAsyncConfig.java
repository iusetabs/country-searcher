package country.searcher;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

public class SpringAsyncConfig implements AsyncConfigurer {

    /*
        @Info: Required for running endpoints on threads for each connection.
     */

    @Override
    public Executor getAsyncExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}

