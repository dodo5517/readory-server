package me.dodo.readingnotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReadingNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingNotesApplication.class, args);
    }

}
