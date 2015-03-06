THIS IS A SAMPLE WORTH HOLDING ON TO.  IT'S NOT USED WITHIN SB-DEPUY AT ALL.

THE SCRIPT TABLES A TABLE HOLDING COMMA-DELIMITED STRINGS AND RETURNS THE DATA

AS A VALID SQL TABLE.







USE [SiteBuilder_custom]

GO

/****** Object:  UserDefinedFunction [dbo].[FN_DEPUY_EVENT_LEAD_SOURCES]    Script Date: 03/30/2008 00:16:57 ******/

SET ANSI_NULLS ON

GO

SET QUOTED_IDENTIFIER ON

GO





CREATE FUNCTION [dbo].[FN_DEPUY_EVENT_LEAD_SOURCES]()



--define the table structure this function will return

RETURNS @tbl TABLE (EVENT_POSTCARD_ID nvarchar(32),

				    STATE_CD nvarchar(5),

				    CITY_NM nvarchar(50) null,

					ZIP_CD nvarchar(50) null) AS



BEGIN

  DECLARE --all variables must be declared before they can be used

	  @pos      int,

      @textpos  int,

      @chunklen smallint,

      @tmpstr   nvarchar(2000),

      @leftover nvarchar(2000),

      @tmpval   nvarchar(2000),

      @list nvarchar(2000),

	  @state_cd nvarchar(5),

      @delimiter nchar(1),

	  @postcardId nvarchar(32)

  SET @delimiter=','

  SET @textpos = 1

  SET @leftover = ''

  



  --loop all entries in the table and create a new table using all the parsed values

  DECLARE rsCursor CURSOR

  FOR Select CITIES_NM, STATE_CD, EVENT_POSTCARD_ID from DEPUY_EVENT_LEAD_SOURCE

  OPEN rsCursor

  FETCH next from rsCursor INTO @list, @state_cd, @postcardId

  WHILE (@@fetch_status = 0)

  BEGIN

	  SET @textpos = 1

	  SET @leftover = ''

	  WHILE @textpos <= datalength(@list) / 2

		BEGIN

			SET @chunklen = 2000 - datalength(@leftover) / 2

			SET @tmpstr = @leftover + substring(@list, @textpos, @chunklen)

			SET @textpos = @textpos + @chunklen

			SET @pos = charindex(@delimiter, @tmpstr)



			WHILE @pos > 0

			  BEGIN

				SET @tmpval = ltrim(rtrim(left(@tmpstr, @pos - 1)))



				--insert the parsed value into the outgoing table

				if len(ltrim(rtrim(@tmpval))) > 0

				  BEGIN

					INSERT @tbl (EVENT_POSTCARD_ID, STATE_CD, CITY_NM, ZIP_CD) 

						VALUES(@postcardId, @state_cd, @tmpval, null)

				  END;



				SET @tmpstr = substring(@tmpstr, @pos + 1, len(@tmpstr))

				SET @pos = charindex(@delimiter, @tmpstr)

			  END



			SET @leftover = @tmpstr

		END



	  --insert the trailing value (after the final delimiter)

	  if len(ltrim(rtrim(@leftover))) > 0

		BEGIN

			INSERT @tbl(EVENT_POSTCARD_ID, STATE_CD, CITY_NM, ZIP_CD)

				VALUES (@postcardId, @state_cd, ltrim(rtrim(@leftover)), null)

		END;



	FETCH next from rsCursor INTO @list, @state_cd, @postcardId

   END

   CLOSE rsCursor

   DEALLOCATE rsCursor



  /*----------------------------------------------------------------------

   * repeat the process and append all Zip Codes to the table.  While this seems silly

   * it avoids having to join multiple tables in our complex queries (where city=? or zip=?)

   *----------------------------------------------------------------------*/



--loop all entries in the table and create a new table using all the parsed values

  DECLARE rsCursor CURSOR

  FOR Select ZIPCODES_TXT, STATE_CD, EVENT_POSTCARD_ID from DEPUY_EVENT_LEAD_SOURCE

  OPEN rsCursor

  FETCH next from rsCursor INTO @list, @state_cd, @postcardId

  WHILE (@@fetch_status = 0)

  BEGIN

	  SET @textpos = 1

	  SET @leftover = ''

	  WHILE @textpos <= datalength(@list) / 2

		BEGIN

			SET @chunklen = 2000 - datalength(@leftover) / 2

			SET @tmpstr = @leftover + substring(@list, @textpos, @chunklen)

			SET @textpos = @textpos + @chunklen

			SET @pos = charindex(@delimiter, @tmpstr)



			WHILE @pos > 0

			  BEGIN

				SET @tmpval = ltrim(rtrim(left(@tmpstr, @pos - 1)))



				--insert the parsed value into the outgoing table

				if len(ltrim(rtrim(@tmpval))) > 0

				  BEGIN

					INSERT @tbl (EVENT_POSTCARD_ID, STATE_CD, CITY_NM, ZIP_CD) 

						VALUES(@postcardId, @state_cd, null, @tmpval)

				  END;



				SET @tmpstr = substring(@tmpstr, @pos + 1, len(@tmpstr))

				SET @pos = charindex(@delimiter, @tmpstr)

			  END



			SET @leftover = @tmpstr

		END



	  --insert the trailing value (after the final delimiter)

	  if len(ltrim(rtrim(@leftover))) > 0

		BEGIN

			INSERT @tbl(EVENT_POSTCARD_ID, STATE_CD, CITY_NM, ZIP_CD)

				VALUES (@postcardId, @state_cd, null, ltrim(rtrim(@leftover)))

		END;



	FETCH next from rsCursor INTO @list, @state_cd, @postcardId

   END

   CLOSE rsCursor

   DEALLOCATE rsCursor



  RETURN

END





