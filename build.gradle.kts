plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7.1"
}

group = "app.mly.plugin.mirai"
version = "0.1.0"

dependencies {
    implementation("cn.hutool:hutool-json:5.7.14")
    implementation("cn.hutool:hutool-http:5.7.14")
}

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}