SELECT
    BSSID,
    SSID,
    room,
    COUNT(*) AS total,
    AVG(level),
    AVG((wifiresult.level - (SELECT AVG(level) FROM wifiresult)) * (level - (SELECT AVG(level) FROM wifiresult))) as variance
FROM wifiresult
WHERE (room = "C6_AISLE6" OR room = "C7_AISLE7")
AND SSID != "TUvisitor" AND SSID != "tudelft-dastud" AND SSID != "Conferentie-TUD"
GROUP BY BSSID, room
ORDER BY AVG(level) DESC