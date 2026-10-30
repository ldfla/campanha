package la.meleva.fundraiser;

import org.springframework.boot.SpringApplication;

public class TestFundraiserApplication {

	public static void main(String[] args) {
		SpringApplication.from(FundraiserApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
