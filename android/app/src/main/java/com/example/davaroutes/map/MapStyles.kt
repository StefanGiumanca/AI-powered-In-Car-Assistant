package com.example.davaroutes.map

object MapStyles {
    const val DARK_MAP_STYLE = """
    [
      {
        "elementType": "geometry",
        "stylers": [{ "color": "#1d2c4d" }]
      },
      {
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#8ec3b9" }]
      },
      {
        "elementType": "labels.text.stroke",
        "stylers": [{ "color": "#1a3646" }]
      },
      {
        "featureType": "road",
        "elementType": "geometry",
        "stylers": [{ "color": "#304a7d" }]
      },
      {
        "featureType": "road",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#98a5be" }]
      },
      {
        "featureType": "water",
        "elementType": "geometry",
        "stylers": [{ "color": "#0e1626" }]
      }
    ]
    """
}