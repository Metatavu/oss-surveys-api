import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.allopen") version "1.7.21"
    id("io.quarkus")
    id("org.openapi.generator") version "7.0.0"
    jacoco
    id("com.github.nbaztec.coveralls-jacoco") version "1.2.14"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val jaxrsFunctionalTestBuilderVersion: String by project
val awaitilityVersion: String by project
val camelPahoVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-kotlin")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-liquibase")
    implementation("io.quarkus:quarkus-reactive-mysql-client")
    implementation("io.quarkus:quarkus-hibernate-reactive-panache")
    implementation("io.quarkus:quarkus-jdbc-mysql")
    implementation("io.quarkus:quarkus-smallrye-health")

    implementation("io.quarkiverse.amazonservices:quarkus-amazon-s3")
    implementation("software.amazon.awssdk:netty-nio-client")

    implementation("commons-codec:commons-codec")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.quarkus:quarkus-smallrye-reactive-messaging-mqtt")
    implementation("io.quarkus:quarkus-scheduler")

    testImplementation("software.amazon.awssdk:url-connection-client")
    testImplementation("org.testcontainers:localstack")
    testImplementation("com.amazonaws:aws-java-sdk-core:1.12.676")

    testImplementation("com.squareup.okhttp3:okhttp")
    testImplementation("fi.metatavu.jaxrs.testbuilder:jaxrs-functional-test-builder:$jaxrsFunctionalTestBuilderVersion")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:mysql")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-jacoco")
    testImplementation("org.testcontainers:hivemq")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    implementation("org.apache.camel.quarkus:camel-quarkus-paho:$camelPahoVersion")
}

group = "fi.metatavu.oss"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets["main"].java {
    srcDir("build/generated/api-spec/src/main/kotlin")
}
sourceSets["test"].java {
    srcDir("build/generated/api-client/src/main/kotlin")
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("javax.enterprise.context.RequestScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("javax.persistence.Entity")
}

coverallsJacoco {
    reportPath = "build/jacoco-report/jacoco.xml"
}

tasks.named("coverallsJacoco") {
    dependsOn(tasks.jacocoTestReport)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    configure<JacocoTaskExtension> {
        excludeClassLoaders = listOf("*QuarkusClassLoader*")
        setDestinationFile(layout.buildDirectory.file("jacoco-quarkus.exec").get().asFile)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}

val generateApiSpec = tasks.register("generateApiSpec",GenerateTask::class){
    setProperty("generatorName", "kotlin-server")
    setProperty("inputSpec",  "$rootDir/oss-surveys-api-spec/swagger.yaml")
    setProperty("outputDir", "$buildDir/generated/api-spec")
    setProperty("apiPackage", "${project.group}.api.spec")
    setProperty("invokerPackage", "${project.group}.api.invoker")
    setProperty("modelPackage", "${project.group}.api.model")
    this.configOptions.put("library", "jaxrs-spec")
    this.configOptions.put("dateLibrary", "java8")
    this.configOptions.put("enumPropertyNaming", "UPPERCASE")
    this.configOptions.put("interfaceOnly", "true")
    this.configOptions.put("useMutiny", "true")
    this.configOptions.put("returnResponse", "true")
    this.configOptions.put("useSwaggerAnnotations", "false")
    this.configOptions.put("additionalModelTypeAnnotations", "@io.quarkus.runtime.annotations.RegisterForReflection")
}

val generateApiClient = tasks.register("generateApiClient",GenerateTask::class){
    setProperty("generatorName", "kotlin")
    setProperty("library", "jvm-okhttp3")
    setProperty("inputSpec",  "$rootDir/oss-surveys-api-spec/swagger.yaml")
    setProperty("outputDir", "$buildDir/generated/api-client")
    setProperty("packageName", "${project.group}.test.client")
    this.configOptions.put("dateLibrary", "string")
    this.configOptions.put("collectionType", "array")
    this.configOptions.put("serializationLibrary", "jackson")
    this.configOptions.put("enumPropertyNaming", "UPPERCASE")
}

tasks.named("compileKotlin") {
    dependsOn(generateApiSpec)
}

tasks.named("compileTestKotlin") {
    dependsOn(generateApiClient)
}

tasks.named<Test>("test") {
    testLogging.showStandardStreams = true
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}
