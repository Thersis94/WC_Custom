@echo off

rem ************ Start Configurable Section *******************
rem This is the ANS lib directory  
set LIB_BASE=C:\Develop\Code\Eclipse\SB_ANS_Medical\lib\

rem ************ End Configurable Section *******************

rem setup the classpaths to the SB libs and the Servlet jar
rem build the command to run the IndexBuilder class
set JAVA_COMMAND=com.ansmed.sb.util.WeeklyReport

set SB_CP=%SB_CP%.;bin;scripts;%LIB_BASE%sitebuilder.jar;
set SB_CP=%SB_CP%%LIB_BASE%siliconmtn.jar;%LIB_BASE%log4j-1.2.8.jar;%LIB_BASE%sqljdbc.jar;
set SB_CP=%SB_CP%;%LIB_BASE%dom4j-1.6.1.jar;%LIB_BASE%jaxen-1.1-beta-6.jar;
set SB_CP=%SB_CP%;%LIB_BASE%commons-net-1.4.1.jar;%LIB_BASE%mail-1.4.jar;


rem Build the index
C:\Develop\java\jre1.6.0_03\bin\java -classpath %SB_CP%; %JAVA_COMMAND%