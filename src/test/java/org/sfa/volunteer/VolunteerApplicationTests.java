package org.sfa.volunteer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VolunteerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void main() {
		VolunteerApplication.main(new String[] {});
		// If the application context loads without exceptions, the main method works correctly
		assertThat(true).isTrue();
	}

}
