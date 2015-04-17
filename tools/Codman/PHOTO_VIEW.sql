USE [Codman_ProdCatalog]

GO

/****** Object:  View [dbo].[PHOTO_VIEW]    Script Date: 05/06/2008 12:55:30 ******/

SET ANSI_NULLS ON

GO

SET QUOTED_IDENTIFIER ON

GO



CREATE VIEW [dbo].[PHOTO_VIEW]

AS

SELECT     a.PHOTO_NM, a.PHOTO_URL, b.PRODUCT_ID

FROM         dbo.PHOTO AS a INNER JOIN

                      dbo.PROD_PHOTO AS b ON a.PHOTO_ID = b.PHOTO_ID



