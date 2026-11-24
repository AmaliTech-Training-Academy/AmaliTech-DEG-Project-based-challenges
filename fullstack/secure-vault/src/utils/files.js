// Known file extensions: readable format name and badge colour group.
const FORMATS = {
  pdf: { format: 'PDF document', kind: 'pdf' },
  doc: { format: 'Word document', kind: 'doc' },
  docx: { format: 'Word document', kind: 'doc' },
  xls: { format: 'Excel spreadsheet', kind: 'sheet' },
  xlsx: { format: 'Excel spreadsheet', kind: 'sheet' },
  csv: { format: 'CSV spreadsheet', kind: 'sheet' },
  png: { format: 'PNG image', kind: 'image' },
  jpg: { format: 'JPEG image', kind: 'image' },
  svg: { format: 'SVG image', kind: 'image' },
  txt: { format: 'Text file', kind: 'other' },
  yaml: { format: 'YAML config', kind: 'code' },
  json: { format: 'JSON data', kind: 'code' },
  ttf: { format: 'TrueType font', kind: 'other' },
};

// "Case_Summary.docx" -> { baseName: "Case_Summary", type: "DOCX", format: "Word document", kind: "doc" }
export function getFileInfo(name) {
  const dot = name.lastIndexOf('.');
  const hasExtension = dot > 0; // ".gitignore" has no extension

  const extension = hasExtension ? name.slice(dot + 1).toLowerCase() : '';
  const known = FORMATS[extension];

  return {
    baseName: hasExtension ? name.slice(0, dot) : name,
    type: extension ? extension.toUpperCase() : 'FILE',
    format: known?.format ?? (extension ? `${extension.toUpperCase()} file` : 'File'),
    kind: known?.kind ?? 'other',
  };
}

// "4.2MB" -> "4.2 MB"
export function formatSize(size) {
  if (!size) return '—';
  return size.replace(/^([\d.]+)\s*([a-zA-Z]+)$/, '$1 $2');
}
