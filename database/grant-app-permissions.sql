USE [BlockFall];
GO

IF DATABASE_PRINCIPAL_ID(N'BlockFallApp') IS NULL
BEGIN
    CREATE USER [BlockFallApp] FOR LOGIN [BlockFallApp];
END
GO

IF IS_ROLEMEMBER(N'db_datareader', N'BlockFallApp') <> 1
BEGIN
    ALTER ROLE [db_datareader] ADD MEMBER [BlockFallApp];
END
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'BlockFallApp') <> 1
BEGIN
    ALTER ROLE [db_datawriter] ADD MEMBER [BlockFallApp];
END
GO

GRANT CREATE TABLE TO [BlockFallApp];
GRANT ALTER ON SCHEMA::[dbo] TO [BlockFallApp];
GO
