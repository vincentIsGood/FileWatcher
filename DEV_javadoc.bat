@echo off

set structure=com.projectName

javadoc -cp src/ -d docs -subpackages %structure%

pause