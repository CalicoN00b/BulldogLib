# BulldogLib

Library of code to be used for the convencience of FRC Team 5933: JudgeMent Call Robotics.

# Adding Repository as a Submodule

To add this repository as a submodule, go to the root robot code directory and type in these commands:

```
git submodule add https://github.com/CalicoN00b/BulldogLib
git submodule update --init --recursive
```

# Adding Submodule to Gradle

In order to make this library, first go to the `settings.gradle` of the root robot code directory, and add these lines at the end:
```gradle

includeBuild('BulldogLib') {
    dependencySubstitution {
        substitute module('net.calicoctl:bulldoglib') using project(':lib')
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
downloadDepsPreemptively.dependsOn gradle.includedBuild('BulldogLib').task(':lib:jar')
```

Finally, rebuild the code from the robot root directory and the library will be ready to use.

# Library Javadocs

## Generating Javadocs Independently

If you are just cloning this repository on its own, you can generate and view the javadocs by typing these two commands into your terminal:

```
.\gradlew.bat javadoc
start .\lib\build\docs\index.html
```

## Generating When Cloned as a Submodule

If you have followed the steps to add this repository as a submodule and to add it to Gradle, you can generate and view the javadocs by typing these two commands into your terminal:

```
.\gradlew.bat :BulldogLib:lib:javadoc
start .\BulldogLib\lib\build\docs\index.html
```