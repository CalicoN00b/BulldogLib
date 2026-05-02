# BulldogLib

Library of code to be used for the convencience of FRC Team 5933: JudgeMent Call Robotics.

# Dependencies

This library relies on some vendordeps that must also be present in the root robot directory in order to function:
- Phoenix6
- AdvantageKit
- REVLib

# Adding Repository as a Git Submodule

To add this repository as a git submodule, go to the root robot code directory and type in these commands:

```
git submodule add https://github.com/CalicoN00b/BulldogLib
git submodule update --init --recursive
```

# Updating Submodule

To update this repository after it has been added as a git submodule, go to the root robot code directory and type this command:

```
git submodule update --remote --recursive
```

# Adding Library to Gradle

In order to use this library, first go to the `settings.gradle` of the root robot code directory, and add these lines at the end:
```gradle

includeBuild('BulldogLib') {
    dependencySubstitution {
        substitute module('net.calicoctl:bulldoglib') using project(':bulldoglib')
    }
}
```

Next, whatever text is in the parentheses of `module` (in this case, `net.calicoctl:bulldoglib`) will need to go in an `implementation` statement in the `build.gradle` of the root robot code directory. It will go in the `dependencies` block.

```gradle
dependencies {
    // ...
    implementation 'net.calicoctl:bulldoglib'
    // ...
}
```

Then, to make sure that the library jars are generated before GradleRIO will refer to them, add this line of code anywhere in the root robot directory's `build.gradle` file:

```gradle
downloadDepsPreemptively.dependsOn gradle.includedBuild('BulldogLib').task(':bulldoglib:jar')
```

Finally, rebuild the code from the robot root directory and the library will be ready to use.

# VSCode Jank

Due to some oddities with VSCode, autocompletion may not work unless you add this line of code to the `.vscode/settings.json` in your root robot code directory:

```
"java.gradle.buildServer.enabled": "off"
```

This is not necessary, but if gradle builds fine but the library doesn't want to work in code, you may want to try this (assuming you followed the rest of the instructions correctly).

# Library Javadocs

## Generating Javadocs Independently

If you are just cloning this repository on its own, you can generate and view the javadocs by typing these two commands into your terminal:

```
# Windows
.\gradlew.bat javadoc
start .\bulldoglib\build\docs\allpackages-index.html

# Mac
./gradlew javadoc
open ./bulldoglib/build/docs/allpackages-index.html

# Linux
./gradlew javadoc
<browser> ./bulldoglib/build/docs/allpackages-index.html # Replace <browser> with your broswer of choice, such as firefox
```

## Generating Javadocs When Cloned as a Submodule

If you have followed the steps to add this repository as a submodule and to add it to Gradle, you can generate and view the javadocs by typing these two commands into your terminal:

```
# Windows
.\gradlew.bat :BulldogLib:bulldoglib:javadoc
start .\BulldogLib\bulldoglib\build\docs\allpackages-index.html

# Mac
./gradlew javadoc
open ./BulldogLib/bulldoglib/build/docs/allpackages-index.html

# Linux
./gradlew javadoc
<browser> ./BulldogLib/bulldoglib/build/docs/allpackages-index.html # Replace <browser> with your broswer of choice, such as firefox
```