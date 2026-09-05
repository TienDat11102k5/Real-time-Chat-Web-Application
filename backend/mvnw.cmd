@echo off
if exist "%M2_HOME%\bin\mvn.cmd" (
    "%M2_HOME%\bin\mvn.cmd" %*
) else if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" (
    "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" %*
) else (
    mvn %*
)
