plugins {
	java
	id("org.springframework.boot") version "2.7.7"
	id("io.spring.dependency-management") version "1.0.15.RELEASE"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2021.0.5"

dependencies {
	implementation("org.springframework.cloud:spring-cloud-stream")
	implementation("org.springframework.cloud:spring-cloud-stream-binder-kinesis:2.2.0")
	implementation("org.springframework.integration:spring-integration-aws:2.5.3")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
