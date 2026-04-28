

Skip to content
Using Gmail with screen readers
Conversations
78% of 15 GB used
Terms · Privacy · Program Policies
Last account activity: 0 minutes ago
Details
# SupportFlow

**A browser-based visual editor for designing, editing, and previewing customer support chatbot conversation flows.**

\---

## Project Overview

SupportFlow is a client-side decision tree editor that lets support teams and product managers build automated chatbot conversation flows through a drag-and-drop canvas interface; no spreadsheets, no backend.

Conversation logic is a directed graph: nodes represent questions or terminal messages, and edges represent the choices a user can take. The editor reads and writes a plain JSON schema, making it portable to any chatbot runtime that can consume structured flow data.

## 

## Features

* **Visual canvas**; Nodes are rendered at absolute positions on a pannel, zoomable canvas. Connections between nodes are drawn as SVG cubic Bézier curves with directional arrowheads and mid-path option labels.
* **Live node editor**; Clicking any node opens a side Inspector Panel. Text edits reflect on the canvas in real time. Options (choices) can be added, re-labelled, re-targeted, or deleted without leaving the panel.
* **Node management**; New nodes can be created via a modal with type selection (Question or End). Deleting a node cascades: all references to that node's ID are cleaned up automatically.
* **Preview mode**; A full-screen chat UI simulates the bot experience end-to-end. Messages animate in as the user selects options. A Restart button appears at terminal nodes.
* **Export JSON**; The current in-memory flow state can be downloaded as `flow\_data.json` at any point, preserving all edits for deployment or version control.
* **Canvas navigation**; Pan by click-dragging, zoom by scroll wheel or toolbar buttons, and fit all nodes to the viewport with a single action.
* **Minimap**; A persistent thumbnail in the lower-left corner shows node positions and a viewport indicator for spatial orientation on large canvases.
* **Connection highlighting**; Selecting a node highlights its incoming and outgoing edges, making it easy to trace paths through complex flows.





## Architecture

SupportFlow is a single-file application (`SupportFlow.html`) with no external runtime dependencies. All logic is written in Vanilla JavaScript; all styling uses plain CSS with custom properties.



### Data model

The application state is a flat array of node objects loaded from `flow\_data.json` at startup:

```json
{
  "nodes": \[
    {
      "id": "1",
      "type": "start | question | end",
      "text": "Message or question text",
      "position": { "x": 500, "y": 50 },
      "options": \[
        { "label": "Choice label", "nextId": "2" }
      ]
    }
  ]
}
```

All edits, additions, deletions operate directly on this array. There is no virtual DOM layer; targeted DOM mutations are applied for performance-sensitive operations (e.g. live text editing updates the relevant element's `textContent` directly rather than re-rendering the full node list).



### Rendering pipeline

```
State mutation
  → renderNodes()       — injects/replaces .node elements on the canvas
  → renderConnections() — rewrites the SVG layer with recalculated Bézier paths
  → renderMinimap()     — repaints the Canvas 2D thumbnail
```

### 

### Connection drawing

Connections are drawn without any graph library. For each `option` on a node:

* **Source anchor**; staggered along the node's bottom edge: `srcX = nodeX + nodeW × (i + 1) / (totalOptions + 1)`
* **Bézier control points**; derived from the vertical distance between source and target: `cp1y = srcY + max(40, Δy × 0.5)`, `cp2y = tgtY − max(40, Δy × 0.3)`
* **Arrowheads**; defined as SVG `<marker>` elements in `<defs>`, with separate variants for default and highlighted states

### 

### Canvas transform

Pan and zoom are implemented via a CSS `transform: translate(panX, panY) scale(scale)` on the canvas inner element, updated on `mousemove` and `wheel` events. Node drag positions are adjusted for the current scale factor to keep mouse and node coordinates in sync.





## Usage

No installation or build step is required.

**Open directly in a browser:**

```bash
open SupportFlow.html
```

**Or serve locally (recommended for consistent behaviour across incognito browsers):**



## 

## Limitations

* **No persistence layer.** All state is in-memory. Closing the browser tab discards unsaved changes. Use Export JSON before closing to preserve work.
* **Single start node.** The application expects exactly one node with `type: "start"`. Preview mode will not start if no start node is present.
* **Orphaned `nextId` references.** If a node is deleted while another node still references it via an option, those options are removed automatically. However, importing a JSON file with pre-existing broken references will cause those connections to silently render nothing.
* **No undo/redo.** Mutations to the flow are immediate and not reversible within the session.
* **No mobile support.** The editor requires a mouse for drag interactions. The Preview (chat) interface is usable on touch devices but the canvas editor is not optimised for touch.
* **Browser compatibility.** Requires a modern browser with support for CSS custom properties, SVG `<marker>`. Internet Explorer is not supported.

README.md
Displaying README.md.
