package uz.itpu.teamwork.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
		classes = ApplicationTests.TestConfig.class,
		properties = "spring.profiles.active=test"
)
class ApplicationTests {

	@Test
	void contextLoads() {
	}

	@Configuration
	@EnableAutoConfiguration(
			exclude = {
					DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class,
					FlywayAutoConfiguration.class,
					MailSenderAutoConfiguration.class
			}
	)
	static class TestConfig {
	}
}
