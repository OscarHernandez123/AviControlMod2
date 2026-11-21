"""Genera el proxy de acceso académico sin entregar claves al navegador."""
import base64
import os
from pathlib import Path

folder = Path(__file__).resolve().parent
root = folder.parent
if root.name != 'avicontrol-modulo2':
    raise SystemExit('Instalar en ~/avicontrol-modulo2/academic-access')
env = dict(line.split('=', 1) for line in (root / '.env').read_text().splitlines()
           if '=' in line and not line.startswith('#'))
text = (folder / 'nginx.academic.conf.template').read_text()
for role, default in [('ADMIN', 'admin'), ('NUTRICIONISTA', 'nutricionista'),
                      ('VETERINARIO', 'veterinario'), ('TRABAJADOR', 'trabajador')]:
    username = env.get(f'AVICONTROL_{role}_USERNAME', default)
    password = env[f'AVICONTROL_{role}_PASSWORD']
    auth = 'Basic ' + base64.b64encode(f'{username}:{password}'.encode()).decode()
    text = text.replace('{{' + role + '_AUTH}}', auth)
output = folder / 'nginx.active.conf'
fd = os.open(output, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, 'w') as handle:
    handle.write(text)
output.chmod(0o600)
print('Proxy académico generado. Las credenciales internas no se muestran ni se incorporan a la imagen.')