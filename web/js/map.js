const API_URL = "/api/mapdata";

let map;
let markers = {}; 

function initMap() {
    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: 9.0060, lng: 38.7640 },
        zoom: 13,
    });
    
    intervalId = setInterval(updateMap, 2000);
    updateMap();
}

var intervalId;

async function updateMap() {
    try {
        const res = await fetch(API_URL);
        const json = await res.json();
        
        if (json && json.payload) {
            // Expose for Admin Panel
            window.latestMapData = json.payload;
            
            const drivers = JSON.parse(json.payload.drivers || "[]");
            const passengers = JSON.parse(json.payload.passengers || "[]");
            
            updateMarkers(drivers, 'driver');
            updateMarkers(passengers, 'passenger');
            
            if (document.getElementById('driver-count')) document.getElementById('driver-count').innerText = drivers.length;
            if (document.getElementById('passenger-count')) document.getElementById('passenger-count').innerText = passengers.length;
            if (document.getElementById('connection-status')) {
                document.getElementById('connection-status').innerText = "Connected";
                document.getElementById('connection-status').style.color = "#10b981";
            }
        }
    } catch (e) {
        document.getElementById('connection-status').innerText = "Connection lost";
        document.getElementById('connection-status').style.color = "red";
    }
}

function updateMarkers(list, type) {
    list.forEach(item => {
        let pos = { lat: item.latitude, lng: item.longitude };
        
        if (markers[item.username]) {
            markers[item.username].setPosition(pos);
        } else {
            let color = type === 'driver' ? (item.available ? 'green' : 'red') : 'blue';
            
            markers[item.username] = new google.maps.Marker({
                position: pos,
                map: map,
                title: item.username,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 8,
                    fillColor: color,
                    fillOpacity: 1,
                    strokeWeight: 2,
                    strokeColor: "white"
                }
            });
            
            const infoWindow = new google.maps.InfoWindow({
                content: item.username
            });

            markers[item.username].addListener("click", () => {
                infoWindow.open(map, markers[item.username]);
            });
        }
    });
}
