import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SRC = process.argv[2] || 'd:/itinerary_202608131325.csv';
const OUT = path.resolve(__dirname, '../src/main/resources/config/liquibase/seed-data');

function parseCsv(text) {
  const rows = [];
  let i = 0;
  let field = '';
  let row = [];
  let inQ = false;
  while (i < text.length) {
    const c = text[i];
    if (inQ) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i += 2;
          continue;
        }
        inQ = false;
        i++;
        continue;
      }
      field += c;
      i++;
      continue;
    }
    if (c === '"') {
      inQ = true;
      i++;
      continue;
    }
    if (c === ',') {
      row.push(field);
      field = '';
      i++;
      continue;
    }
    if (c === '\r') {
      i++;
      continue;
    }
    if (c === '\n') {
      row.push(field);
      rows.push(row);
      field = '';
      row = [];
      i++;
      continue;
    }
    field += c;
    i++;
  }
  if (field.length || row.length) {
    row.push(field);
    rows.push(row);
  }
  return rows;
}

function slug(s) {
  return (
    s
      .normalize('NFD')
      .replace(/\p{M}/gu, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D')
      .replace(/[^A-Za-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .toUpperCase() || 'X'
  );
}

function esc(s) {
  return String(s ?? '')
    .replace(/;/g, ',')
    .replace(/"/g, '');
}

const raw = fs.readFileSync(SRC, 'utf8');
const rows = parseCsv(raw);
const headers = rows[0].map(h => h.replace(/^"|"$/g, '').trim());
const data = rows
  .slice(1)
  .filter(r => r.length > 1 && (r[0] || '').trim())
  .map(r => Object.fromEntries(headers.map((h, i) => [h, (r[i] ?? '').trim()])));

const EXCLUDE_BRANCHES = new Set(['Nội Bài']);
const branchNames = [...new Set(data.map(d => d.branch_name).filter(n => n && !EXCLUDE_BRANCHES.has(n)))].sort((a, b) =>
  a.localeCompare(b, 'vi'),
);
const knownBranches = new Set(branchNames);
const branchMap = new Map();
const branchLines = ['id;code;name;active'];
branchNames.forEach((name, idx) => {
  const id = idx + 1;
  branchMap.set(name, id);
  branchLines.push([id, slug(name), esc(name), 'true'].join(';'));
});

/** Source CSV sometimes has wrong branch_name; prefer dest/depart if they are a known Tuyến. */
function resolveBranchName(d) {
  if (knownBranches.has(d.destination_point)) return d.destination_point;
  if (knownBranches.has(d.departure_point)) return d.departure_point;
  if (d.branch_name && knownBranches.has(d.branch_name)) return d.branch_name;
  return null;
}

const itinLines = [
  'id;code;name;branch_id;departure_point;destination_point;route_direction;route_type;price;priority;display_order;active;shortest_itinerary',
];
for (const d of data) {
  if (!d.id) continue;
  const branchName = resolveBranchName(d);
  if (!branchName) continue;
  const name = esc(d.name);
  itinLines.push(
    [
      d.id,
      slug(d.name) || `IT${d.id}`,
      name,
      branchMap.get(branchName),
      esc(d.departure_point),
      esc(d.destination_point),
      esc(d.route_direction),
      d.route_type || '0',
      d.price || '0',
      d.priority || '0',
      d.display_order || '0',
      d.status === '1' ? 'true' : 'false',
      esc(d.shortest_itinerary),
    ].join(';'),
  );
}

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, 'branch.csv'), branchLines.join('\n') + '\n', 'utf8');
fs.writeFileSync(path.join(OUT, 'itinerary.csv'), itinLines.join('\n') + '\n', 'utf8');
console.log(
  JSON.stringify(
    {
      branches: branchNames.length,
      itineraries: itinLines.length - 1,
      sampleBranches: branchLines.slice(0, 5),
      out: OUT,
    },
    null,
    2,
  ),
);
