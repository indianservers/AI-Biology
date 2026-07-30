plugins {
    alias(libs.plugins.android.application)
}

val biologyModelCatalog = listOf(
    "Bacteriacell.glb",
    "Cell Membrane.glb",
    "Chloroplast.glb",
    "epithelial microvilli.glb",
    "Lysosome.glb",
    "Mitochondrion.glb",
    "Neuron.glb",
    "plant cell wall.glb",
    "PlantCell.glb",
    "Ribosomes.glb",
    "Rough Endoplasmic Reticulum.glb",
    "Smooth Endoplasmic Reticulum.glb",
    "Vacuole.glb",
    "WhiteBloodCell.glb"
)
val bundledBiologyModels = setOf(
    "Bacteriacell.glb",
    "Neuron.glb",
    "Vacuole.glb"
)
val omitBundledModels =
    providers.gradleProperty("omitBundledModels").map(String::toBoolean).getOrElse(true)
val biologyCatalogUrl =
    providers.gradleProperty("biologyCatalogUrl").getOrElse("")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
val modelsExcludedFromApk =
    if (omitBundledModels) biologyModelCatalog else biologyModelCatalog - bundledBiologyModels

android {
    namespace = "com.indianservers.biology"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.indianservers.biology"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BIOLOGY_CATALOG_URL", "\"$biologyCatalogUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    androidResources {
        noCompress += "glb"
        ignoreAssetsPattern =
            if (omitBundledModels) "*.glb" else modelsExcludedFromApk.joinToString(":")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val verifyBundledBiologyModels by tasks.registering {
    val modelDirectory = layout.projectDirectory.dir("src/main/assets/biology/3d")
    inputs.dir(modelDirectory)

    doLast {
        if (omitBundledModels) return@doLast
        val missingModels = bundledBiologyModels.filter { modelName ->
            val modelFile = modelDirectory.file(modelName).asFile
            !modelFile.isFile || modelFile.length() == 0L
        }
        check(missingModels.isEmpty()) {
            "Cannot build without bundled biology models: ${missingModels.joinToString()}"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyBundledBiologyModels)
}
