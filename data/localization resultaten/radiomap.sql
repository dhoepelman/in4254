SELECT  BSSID, room, AVG(level)
FROM wifiresult
GROUP BY room, BSSID
ORDER BY room, BSSID