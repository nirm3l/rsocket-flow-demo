package com.flow.demo.broker;

import org.roaringbitmap.RoaringBitmap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BrokerApplication {
	public static void main(String[] args) {
		RoaringBitmap bitmap = RoaringBitmap.bitmapOf(1,2,3);

		SpringApplication.run(BrokerApplication.class, args);
	}
}
