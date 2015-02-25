echo off

rem ************ Start Configurable Section *******************
rem This is the SB lib directory  
set CORE_LIB_BASE=S:\java-sites\SiteBuilderII\shared_libs\
set LIB_BASE=S:\java-sites\SiteBuilderII\WebRoot\WEB-INF\lib\

rem Change these params as necessary
set INDEX_DIR=S:\java-sites\SiteBuilderII\index\Codman_ProdCatalog
set DB_USER=sb_user
set DB_PWD=sqll0gin
set DB_URL=jdbc:sqlserver://10.0.20.43:2007
set DB_SCHEMA=Codman_ProdCatalog.dbo.
set ORG_NM=Codman
set LOG4J_FILE=D:\\log4j.properties

rem ************ End Configurable Section *******************


rem ************ Do Not change below here *******************
rem build the command to run the IndexBuilder class
rem Usage: java IndexBuilder [Index Dir] [DB User] [DB Password] [DB URL] [SCHEMA_NM] [PROD-CAT ORGANIZATION]
set JAVA_COMMAND=com.codman.catalog.lucene.IndexBuilder %INDEX_DIR% %DB_USER% %DB_PWD% %DB_URL% %DB_SCHEMA% %ORG_NM% %LOG4J_FILE%

rem setup the classpaths to the SB libs and the Servlet jar
set SB_CP=D:\develop\apache-tomcat-6.0.16\lib\sqljdbc.jar;%LIB_BASE%siliconmtn.jar;%CORE_LIB_BASE%lucene-core-2.0.0.jar;
set SB_CP=%SB_CP%;%LIB_BASE%depuy_sb.jar;%CORE_LIB_BASE%log4j-1.2.15.jar;

rem Build the index
java -classpath %SB_CP% %JAVA_COMMAND%
