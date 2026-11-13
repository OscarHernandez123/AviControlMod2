import { formatRation } from '../../utils/nutritionUtils';

const FEED_TYPES = [
  'Alimento A (Pre-iniciador)',
  'Alimento B (Iniciador)',
  'Alimento E (Iniciador Plus)',
  'Alimento B (Iniciador Medicado)',
  'Alimento C (Broiler)',
  'Alimento D (Broiler Alta Densidad)'
];

function StagesEditor({ stages = [], onChange }) {
  const handleStageFieldChange = (index, field, value) => {
    const next = stages.map((st, idx) => {
      if (idx !== index) return st;
      if (field === 'startDay' || field === 'endDay') {
        const parsed = value === '' ? '' : parseInt(value, 10);
        return { ...st, [field]: Number.isNaN(parsed) ? '' : parsed };
      }
      if (field === 'rationKg') {
        return { ...st, rationKg: value };
      }
      return { ...st, [field]: value };
    });
    onChange(next);
  };

  const handleAddStage = () => {
    const lastStage = stages[stages.length - 1];
    const nextStart = lastStage ? (Number(lastStage.endDay) || 1) + 1 : 1;
    const nextEnd = nextStart + 13;
    const newStage = {
      id: `st-${Date.now()}-${stages.length + 1}`,
      name: `Etapa ${stages.length + 1}`,
      startDay: nextStart,
      endDay: nextEnd,
      feedType: FEED_TYPES[Math.min(stages.length, FEED_TYPES.length - 1)],
      rationKg: 0.065
    };
    onChange([...stages, newStage]);
  };

  const handleRemoveStage = (index) => {
    if (stages.length <= 1) return;
    const next = stages.filter((_, idx) => idx !== index);
    onChange(next);
  };

  const handleAutoFixSequence = () => {
    let cursor = 1;
    const fixed = stages.map((st) => {
      const duration = Math.max(
        1,
        (Number(st.endDay) || cursor) - (Number(st.startDay) || cursor)
      );
      const startDay = cursor;
      const endDay = startDay + duration;
      cursor = endDay + 1;
      return {
        ...st,
        startDay,
        endDay
      };
    });
    onChange(fixed);
  };

  return (
    <div style={{ gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '8px'
        }}
      >
        <label className="avi-figma-label" style={{ margin: 0 }}>
          Etapas del plan nutricional ({stages.length})
        </label>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            className="avi-admin-table-btn avi-admin-table-btn-outline"
            style={{ height: '30px', padding: '0 10px', fontSize: '12px' }}
            onClick={handleAutoFixSequence}
          >
            Ajustar días consecutivos
          </button>
          <button
            type="button"
            className="avi-admin-table-btn avi-admin-table-btn-green"
            style={{ height: '30px', padding: '0 12px', fontSize: '12px' }}
            onClick={handleAddStage}
          >
            ＋ Agregar etapa
          </button>
        </div>
      </div>

      {/* Cabecera compacta de la tabla de etapas */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1.2fr 0.65fr 0.65fr 1.45fr 0.95fr auto',
          gap: '8px',
          padding: '0 10px',
          fontSize: '12px',
          fontWeight: 600,
          color: '#606367'
        }}
      >
        <span>Nombre de etapa</span>
        <span>Día inicio</span>
        <span>Día fin</span>
        <span>Tipo de alimento</span>
        <span>Ración (kg/ave/día)</span>
        <span style={{ width: stages.length > 1 ? '32px' : '0px' }} />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {stages.map((st, idx) => (
          <div
            key={st.id || idx}
            style={{
              backgroundColor: '#faf6ee',
              border: '1px solid #dfd8ca',
              borderRadius: '10px',
              padding: '8px 10px',
              display: 'grid',
              gridTemplateColumns: '1.2fr 0.65fr 0.65fr 1.45fr 0.95fr auto',
              gap: '8px',
              alignItems: 'center'
            }}
          >
            <input
              type="text"
              className="avi-figma-input"
              style={{ height: '36px', padding: '0 10px', fontSize: '13px' }}
              value={st.name}
              placeholder="Ej. Pre-Inicio"
              onChange={(e) => handleStageFieldChange(idx, 'name', e.target.value)}
              required
            />

            <input
              type="number"
              min="1"
              max="120"
              className="avi-figma-input"
              style={{ height: '36px', padding: '0 10px', fontSize: '13px' }}
              value={st.startDay}
              onChange={(e) => handleStageFieldChange(idx, 'startDay', e.target.value)}
              required
            />

            <input
              type="number"
              min="1"
              max="120"
              className="avi-figma-input"
              style={{ height: '36px', padding: '0 10px', fontSize: '13px' }}
              value={st.endDay}
              onChange={(e) => handleStageFieldChange(idx, 'endDay', e.target.value)}
              required
            />

            <div className="avi-figma-select-wrap">
              <select
                className="avi-figma-input avi-figma-select"
                style={{ height: '36px', padding: '0 28px 0 10px', fontSize: '13px' }}
                value={st.feedType || FEED_TYPES[0]}
                onChange={(e) => handleStageFieldChange(idx, 'feedType', e.target.value)}
              >
                {FEED_TYPES.map((ft) => (
                  <option key={ft} value={ft}>
                    {ft}
                  </option>
                ))}
              </select>
              <img
                alt=""
                src="/assets/admin-select-chevron.svg"
                className="avi-figma-select-chevron"
              />
            </div>

            <input
              type="number"
              step="0.001"
              min="0.001"
              max="1"
              className="avi-figma-input"
              style={{ height: '36px', padding: '0 10px', fontSize: '13px' }}
              value={st.rationKg}
              title={formatRation(st.rationKg)}
              onChange={(e) => handleStageFieldChange(idx, 'rationKg', e.target.value)}
              required
            />

            {stages.length > 1 ? (
              <button
                type="button"
                onClick={() => handleRemoveStage(idx)}
                title="Eliminar etapa"
                style={{
                  width: '32px',
                  height: '36px',
                  background: '#ffe0e3',
                  color: '#b91c1c',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '15px',
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                ×
              </button>
            ) : (
              <span />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

export default StagesEditor;
