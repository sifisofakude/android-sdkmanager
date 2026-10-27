# Android SDK Manager

A lightweight Java library for downloading Android SDK platforms directly from Google's Android SDK repository.

## Features

- Repository configuration: Download Google's Android SDK repository metadata.
- Package discovery: Parse repository XML to retrieve package information.
- Platform downloads: Download Android SDK platforms by repository package path.
- Checksum verification: Verify downloaded archives against repository checksums.
- Archive extraction: Extract downloaded packages into a specified directory.
- Command-line support: Download repository metadata and SDK platforms from the terminal.
- Java API: Integrate SDK management into any Java application.

## Installation

Add the library to your project using its published Maven coordinates, or include it as a local module.

## Usage

**Download an Android SDK Platform**

```java
import io.github.sifisofakude.sdkmanager.AndroidSdkManager;

public class Main {

    public static void main(String[] args) throws Exception {
        AndroidSdkManager sdkmanager = new AndroidSdkManager();

        boolean success = sdkmanager.downloadPlatform(
            "platforms;android-36",
            "./AndroidSdk/platforms"
        );

        if (success) {
            System.out.println("Platform downloaded successfully.");
        } else {
            System.err.println("Platform download failed.");
        }
    }
}
```

The first argument is the package path from Google's repository. The second is the destination directory.

**Download Repository Configuration**

Download the Android SDK repository XML to a local file:

```java
AndroidSdkManager sdkmanager = new AndroidSdkManager();

boolean success = sdkmanager.downloadRepoConfig(
    "./repository.xml"
);
```

Alternatively, retrieve the repository configuration as an input stream:

```java
try (InputStream in = sdkmanager.downloadRepoConfig()) {
    if (in != null) {
        // Process the repository XML.
    }
}
```

**Parse Platform Metadata**

Retrieve metadata for a specific Android SDK platform:

```java
import java.io.InputStream;
import java.util.Map;

AndroidSdkManager sdkmanager = new AndroidSdkManager();

try (InputStream in = sdkmanager.downloadRepoConfig()) {
    if (in != null) {
        Map<String, String> platformInfo =
            sdkmanager.parsePlatformsRepoFile(
                "platforms;android-36",
                in
            );

        System.out.println(platformInfo);
    }
}
```

The returned map contains metadata from the package's "<complete>" element, including the 
download URL, checksum, and archive size when available.

## Command-Line Usage

**Download Repository Configuration**

```bash
java -jar sdkmanager.jar \
    --repo-config \
    --output repository.xml
```

**Download an Android SDK Platform**

```bash
java -jar sdkmanager.jar \
    --platform "platforms;android-36" \
    --output "./AndroidSdk/platforms/android-36"
```

**Display Help**

`java -jar sdkmanager.jar --help`

**How It Works**

The library retrieves Google's Android SDK repository metadata to locate the requested package. It then downloads the package archive, verifies its checksum when provided, and extracts the archive into the destination directory.

## Scope

The library focuses on downloading Android SDK platforms and repository metadata.

License acceptance and management are outside its current scope.

## License

See the repository's "LICENSE" file for licensing information.


You can add your actual Maven coordinates to the installation section once the library is published.
