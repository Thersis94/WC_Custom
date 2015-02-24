USE [Codman_ProdCatalog]

GO

/****** Object:  View [dbo].[SIZE_VIEW]    Script Date: 05/06/2008 12:56:01 ******/

SET ANSI_NULLS ON

GO

SET QUOTED_IDENTIFIER ON

GO



CREATE VIEW [dbo].[SIZE_VIEW]

AS

SELECT     b.CATALOG_NO_ID, a.MEASUREMENT_TXT, c.UNIT_TXT, b.SIZE_TXT

FROM         dbo.MEASUREMENT AS a INNER JOIN

                      dbo.SIZE AS b ON a.MEASUREMENT_ID = b.MEASUREMENT_ID INNER JOIN

                      dbo.UNIT AS c ON b.UNIT_ID = c.UNIT_ID



