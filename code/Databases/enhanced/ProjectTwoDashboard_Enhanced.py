"""Enhanced CS-340 Grazioso Salvare dashboard.

Run this file in the same folder as CRUD_Python_Module.py. The dashboard uses
MongoDB environment variables instead of hardcoded credentials. See .env.example
or README.md for setup values.
"""

from __future__ import annotations

import base64
import copy
from pathlib import Path
from typing import Any, Dict, List, Mapping

import dash_leaflet as dl
import pandas as pd
import plotly.express as px
from dash import Dash, dash_table, dcc, html
from dash.dependencies import Input, Output

from CRUD_Python_Module import AnimalShelter, DEFAULT_PROJECTION

DASHBOARD_TITLE = "CS-340 Grazioso Salvare Dashboard - Matthew Schaub"
DEFAULT_LIMIT = 250
APP_PORT = 8051

FILTER_QUERIES: Dict[str, Dict[str, Any]] = {
    "ALL": {},
    "WATER": {
        "animal_type": "Dog",
        "$or": [
            {"breed": {"$regex": "newf", "$options": "i"}},
            {"breed": {"$regex": "chesa", "$options": "i"}},
            {"breed": {"$regex": "lab", "$options": "i"}},
        ],
        "sex_upon_outcome": "Intact Female",
        "age_upon_outcome_in_weeks": {"$gte": 26, "$lte": 156},
    },
    "MOUNTAIN": {
        "animal_type": "Dog",
        "$or": [
            {"breed": {"$regex": "german", "$options": "i"}},
            {"breed": {"$regex": "mala", "$options": "i"}},
            {"breed": {"$regex": "old english", "$options": "i"}},
            {"breed": {"$regex": "husk", "$options": "i"}},
            {"breed": {"$regex": "rott", "$options": "i"}},
        ],
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome_in_weeks": {"$gte": 26, "$lte": 156},
    },
    "DISASTER": {
        "animal_type": "Dog",
        "$or": [
            {"breed": {"$regex": "german", "$options": "i"}},
            {"breed": {"$regex": "golden", "$options": "i"}},
            {"breed": {"$regex": "blood", "$options": "i"}},
            {"breed": {"$regex": "dober", "$options": "i"}},
            {"breed": {"$regex": "rott", "$options": "i"}},
        ],
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome_in_weeks": {"$gte": 20, "$lte": 300},
    },
}

DISPLAY_COLUMNS: List[str] = [field for field, shown in DEFAULT_PROJECTION.items() if shown and field != "_id"]


def get_approved_query(filter_type: str) -> Dict[str, Any]:
    """Return a copy of a pre-approved query to avoid accepting raw user input."""
    if filter_type not in FILTER_QUERIES:
        filter_type = "ALL"
    return copy.deepcopy(FILTER_QUERIES[filter_type])


def make_columns(frame: pd.DataFrame) -> List[Dict[str, Any]]:
    """Build Dash table column definitions from a dataframe."""
    return [
        {"name": column.replace("_", " ").title(), "id": column, "deletable": False, "selectable": True}
        for column in frame.columns
    ]


def records_to_dataframe(records: List[Mapping[str, Any]]) -> pd.DataFrame:
    """Create a consistently shaped dataframe even when a query returns no records."""
    frame = pd.DataFrame.from_records(records)
    for column in DISPLAY_COLUMNS:
        if column not in frame.columns:
            frame[column] = None
    return frame[DISPLAY_COLUMNS]


def load_logo() -> html.Div:
    """Load the Grazioso Salvare logo when it is present, with a text fallback."""
    logo_path = Path("Grazioso Salvare Logo.png")
    if logo_path.exists():
        encoded_image = base64.b64encode(logo_path.read_bytes()).decode()
        return html.Div(
            html.A(
                html.Img(src=f"data:image/png;base64,{encoded_image}", height=160),
                href="https://www.snhu.edu",
                target="_blank",
            ),
            style={"textAlign": "center"},
        )
    return html.Div("Grazioso Salvare", style={"textAlign": "center", "fontWeight": "bold", "fontSize": "24px"})


db = AnimalShelter()
initial_df = records_to_dataframe(
    db.read({}, projection=DEFAULT_PROJECTION, limit=DEFAULT_LIMIT, sort=[("animal_id", 1)])
)

app = Dash(__name__)
server = app.server

app.layout = html.Div(
    [
        load_logo(),
        html.H1(DASHBOARD_TITLE, style={"textAlign": "center"}),
        html.P(
            "Use the rescue profile filter to review matching shelter dogs, analyze breed counts, "
            "and view the selected animal location.",
            style={"textAlign": "center"},
        ),
        html.Hr(),
        dcc.RadioItems(
            id="filter-type",
            options=[
                {"label": "Reset", "value": "ALL"},
                {"label": "Water Rescue", "value": "WATER"},
                {"label": "Mountain or Wilderness Rescue", "value": "MOUNTAIN"},
                {"label": "Disaster Rescue or Individual Tracking", "value": "DISASTER"},
            ],
            value="ALL",
            inline=True,
            style={"marginTop": "8px", "marginBottom": "8px", "textAlign": "center"},
        ),
        html.Div(id="record-count", style={"textAlign": "center", "fontWeight": "bold"}),
        html.Hr(),
        dash_table.DataTable(
            id="datatable-id",
            columns=make_columns(initial_df),
            data=initial_df.to_dict("records"),
            editable=False,
            page_current=0,
            page_size=10,
            page_action="native",
            sort_action="native",
            filter_action="native",
            row_selectable="single",
            selected_rows=[0] if len(initial_df) else [],
            selected_columns=[],
            style_table={"overflowX": "auto"},
            style_cell={"textAlign": "left", "padding": "6px", "fontFamily": "Arial", "fontSize": 12},
            style_header={"fontWeight": "bold"},
        ),
        html.Br(),
        html.Hr(),
        html.Div(
            className="row",
            style={"display": "flex", "gap": "20px", "alignItems": "flex-start"},
            children=[
                html.Div(id="graph-id", className="col s12 m6", style={"width": "45%"}),
                html.Div(id="map-id", className="col s12 m6", style={"width": "55%"}),
            ],
        ),
        html.Div(
            "Enhanced database version: credentials are read from environment variables, "
            "queries are selected from approved filters, MongoDB projections reduce payload size, "
            "indexes support common filters, and breed counts use aggregation.",
            style={"fontSize": "12px", "marginTop": "20px", "color": "#555"},
        ),
    ],
    style={"maxWidth": "1200px", "margin": "0 auto", "fontFamily": "Arial"},
)


@app.callback(
    [Output("datatable-id", "data"), Output("datatable-id", "columns"), Output("record-count", "children")],
    Input("filter-type", "value"),
)
def update_dashboard(filter_type: str):
    query = get_approved_query(filter_type)
    records = db.read(query, projection=DEFAULT_PROJECTION, limit=DEFAULT_LIMIT, sort=[("animal_id", 1)])
    frame = records_to_dataframe(records)
    count_message = f"Showing {len(frame)} record(s). A maximum of {DEFAULT_LIMIT} records is loaded for dashboard performance."
    return frame.to_dict("records"), make_columns(frame), count_message


@app.callback(Output("graph-id", "children"), Input("filter-type", "value"))
def update_graphs(filter_type: str):
    query = get_approved_query(filter_type)
    summary = db.aggregate_breeds(query, limit=10)
    frame = pd.DataFrame.from_records(summary)

    if frame.empty:
        return html.Div("No breed data is available for this filter.")

    figure = px.pie(frame, names="breed", values="count", title="Top Breed Distribution")
    return dcc.Graph(figure=figure)


@app.callback(
    Output("datatable-id", "style_data_conditional"),
    Input("datatable-id", "selected_columns"),
)
def update_styles(selected_columns):
    return [
        {"if": {"column_id": column}, "backgroundColor": "#D2F3FF"}
        for column in (selected_columns or [])
    ]


@app.callback(
    Output("map-id", "children"),
    [Input("datatable-id", "derived_virtual_data"), Input("datatable-id", "derived_virtual_selected_rows")],
)
def update_map(view_data, selected_rows):
    if not view_data:
        return html.Div("No map data is available.")

    frame = pd.DataFrame.from_records(view_data)
    row_number = selected_rows[0] if selected_rows else 0
    if row_number >= len(frame):
        row_number = 0

    row = frame.iloc[row_number]
    lat = row.get("location_lat")
    lon = row.get("location_long")
    if pd.isna(lat) or pd.isna(lon):
        return html.Div("The selected animal does not have valid location data.")

    animal_name = row.get("name") or "Unknown"
    breed = row.get("breed") or "Unknown breed"
    animal_id = row.get("animal_id") or "Unknown ID"

    return dl.Map(
        style={"width": "100%", "height": "500px"},
        center=[lat, lon],
        zoom=10,
        children=[
            dl.TileLayer(id="base-layer-id"),
            dl.Marker(
                position=[lat, lon],
                children=[
                    dl.Tooltip(str(breed)),
                    dl.Popup([html.H3(str(animal_name)), html.P(f"Animal ID: {animal_id}"), html.P(str(breed))]),
                ],
            ),
        ],
    )


if __name__ == "__main__":
    app.run(debug=True, port=APP_PORT)
