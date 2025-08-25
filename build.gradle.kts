plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "app.titech"
            artifactId = "science-tokyo-portal"
            version = "1.0.0"

            from(components["java"])

            pom {
                name.set(artifactId)
                description.set("Science Tokyo Portal Scraping for Kotlin")
                url.set("https://github.com/TitechAppProject/science-tokyo-portal-kotlin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("nanashiki")
                        name.set("Maruyama Moto")
                        email.set("nanashiki.app@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:TitechAppProject/science-tokyo-portal-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:TitechAppProject/science-tokyo-portal-kotlin.git")
                    url.set("https://github.com/TitechAppProject/science-tokyo-portal-kotlin")
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossrh-staging-api"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            val ossrhUsername: String by project
            val ossrhPassword: String by project
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    val signingKey: String by project
    val signingPassword: String by project
    useInMemoryPgpKeys(
        signingKey,
        signingPassword
    )
    sign(publishing.publications["maven"])
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // HTML Parsing
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}