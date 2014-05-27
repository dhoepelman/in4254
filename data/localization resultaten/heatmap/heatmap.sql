SELECT BSSID , ord, room , AVG ( level )
FROM wifiresult
JOIN roomordering ON room = r
GROUP BY BSSID, room
ORDER BY BSSID, ord