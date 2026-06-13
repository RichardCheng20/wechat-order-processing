package com.vwholesale;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vwholesale.**.mapper")
public class VegetableWholesaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(VegetableWholesaleApplication.class, args);
    }
}
