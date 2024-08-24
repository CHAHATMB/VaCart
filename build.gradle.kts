// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
//    id("dagger.hilt.android.plugin") version "1.0.0-alpha03" apply false
    id("com.squareup.wire") version "4.7.0" apply false
}

//buildscript {
//    repositories {
//        mavenCentral()
//    }
//    dependencies {
//        classpath 'com.squareup.wire:wire-gradle-plugin:4.7.0'
//    }
//}
//
//apply plugin: 'com.squareup.wire'