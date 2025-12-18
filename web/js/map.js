const API_URL = "/api/mapdata";
// REPLACE WITH YOUR GEBETA API KEY
const GEBETA_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55bmFtZSI6ImRlYnJlIGJlcmhhbiB1bml2ZXJzaXR5IiwiZGVzY3JpcHRpb24iOiI5YTkzOTJiNy1mMzQxLTQ3MTctOTMzYy1lMzJkNGIzYmJjMjkiLCJpZCI6IjlkMjBlNmJkLTEzOWQtNGM3OC05ZTBlLWZlZGUzZjQ1ZWZkZSIsImlzc3VlZF9hdCI6MTc2NTkxNDE2NiwiaXNzdWVyIjoiaHR0cHM6Ly9tYXBhcGkuZ2ViZXRhLmFwcCIsImp3dF9pZCI6IjAiLCJzY29wZXMiOlsiRkVBVFVSRV9BTEwiXSwidXNlcm5hbWUiOiJTdGF5SGVyZSJ9.mcVl25CuabEJSFGwFz4O8N6OznOO4dZS1Kb0hSO35_I";

let map;
let markers = {}; 

function initMap() {
    map = L.map('map').setView([9.0060, 38.7640], 13);
    
    // Fallback to OSM
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap',
        maxZoom: 19
    }).addTo(map);
    
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
        let pos = [item.latitude, item.longitude];
        
        if (markers[item.username]) {
            markers[item.username].setLatLng(pos);
        } else {
            let color = type === 'driver' ? (item.available ? 'green' : 'red') : 'blue';
            let icon = createIcon(color);
                
            markers[item.username] = L.marker(pos, { icon: icon })
                .addTo(map)
                .bindPopup(item.username);
        }
    });
}

function createIcon(color) {
    return L.divIcon({
        className: 'custom-icon',
        html: `<div style="background:${color};width:20px;height:20px;border-radius:50%;border:2px solid white;box-shadow:0 0 5px black;"></div>`,
        iconSize: [20, 20]
    });
}

// Start
initMap();
