@echo off
chcp 65001 > nul
echo ===============================================================================
echo   TP CACHE DE SECOND NIVEAU & REQUETES N+1 HIBERNATE - HOUDA EL AMRANI
echo   APPLICATION : BIBLIOTHEQUE NUMERIQUE & MAISON D'EDITION
echo ===============================================================================
call mvn compile exec:java
pause
