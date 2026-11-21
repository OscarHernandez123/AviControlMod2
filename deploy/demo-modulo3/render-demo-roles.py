"""Renderiza exclusivamente el proxy DEMO; nunca imprime credenciales."""
import base64
import os
from pathlib import Path

root = Path(__file__).resolve().parent
if root.name != 'demo-modulo3':
    raise SystemExit('Ejecutar desde el directorio demo-modulo3')
env = dict(line.split('=', 1) for line in (root / '.env').read_text().splitlines()
           if '=' in line and not line.startswith('#'))
text = (root / 'nginx.roles.conf.template').read_text()
for role, default in [('ADMIN', 'admin'), ('NUTRICIONISTA', 'nutricionista'),
                      ('VETERINARIO', 'veterinario'), ('TRABAJADOR', 'trabajador')]:
    username = env.get(f'AVICONTROL_{role}_USERNAME', default)
    password = env[f'AVICONTROL_{role}_PASSWORD']
    auth = 'Basic ' + base64.b64encode(f'{username}:{password}'.encode()).decode()
    text = text.replace('{{' + role + '_AUTH}}', auth)
output = root / 'nginx.roles.active.conf'
fd = os.open(output, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, 'w') as handle:
    handle.write(text)
output.chmod(0o600)
print('Proxy de roles DEMO generado; credenciales conservadas solo en el servidor.')