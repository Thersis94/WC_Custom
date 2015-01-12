INSERT INTO SITEBUILDER_CUSTOM.DBO.ANS_SALES_REP
(SALES_REP_ID, FIRST_NM, LAST_NM, EMAIL_ADDRESS_TXT,
ANS_LOGIN_ID, REGION_ID)
SELECT CAST(A.OBJECTID AS VARCHAR(32)), FIRSTNAME,CAST(LASTNAME AS VARCHAR(40)), EMAIL, 
CAST(LOWER(SUBSTRING(FIRSTNAME,1,1) + LASTNAME) AS VARCHAR(40)),NEW_ID
FROM OBJ_SALESREP A LEFT OUTER JOIN OBJ_TERRITORY B
ON A.TERRITORY = B.TERRITORYNAME
WHERE NEW_ID IS NOT NULL