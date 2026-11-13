// Utilidades de cálculo, formato y validación para el módulo de Nutricionista

export function formatBirds(num) {
  const n = Number(num) || 0;
  return `${n.toLocaleString('es-CO')} aves`;
}

export function formatRation(rationKg) {
  const n = Number(rationKg) || 0;
  return `${n.toFixed(3)} kg/pollo/día`;
}

export function formatKg(kg, decimals = 1) {
  const n = Number(kg) || 0;
  return `${n.toLocaleString('es-CO', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })} kg`;
}

export function formatSacks(sacks, decimals = 2) {
  const n = Number(sacks) || 0;
  return `${n.toLocaleString('es-CO', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })} bultos`;
}

export function formatAgeWeeks(ageDays) {
  const d = Math.max(1, parseInt(ageDays, 10) || 1);
  const weeks = Math.floor(d / 7);
  const remDays = d % 7;
  if (weeks === 0) return `(${remDays} días)`;
  if (remDays === 0) return `(${weeks} sem)`;
  return `(${weeks} sem ${remDays} días)`;
}

export function formatStageDays(stage) {
  const start = Number(stage.startDay) || 1;
  const end = Number(stage.endDay) || start;
  if (stage.extendedDays && stage.extendedDays > 0) {
    return `Días ${start} a ${end} (+${stage.extendedDays} días)`;
  }
  return `Días ${start} a ${end}`;
}

export function findStageForAge(plan, ageDays) {
  if (!plan || !Array.isArray(plan.stages) || plan.stages.length === 0) {
    return {
      name: 'Inicio',
      startDay: 1,
      endDay: 21,
      feedType: 'Alimento Estándar',
      rationKg: 0.045
    };
  }
  const age = Number(ageDays) || 1;
  const matched = plan.stages.find(
    (st) => age >= Number(st.startDay) && age <= Number(st.endDay)
  );
  if (matched) return matched;
  if (age < Number(plan.stages[0].startDay)) return plan.stages[0];
  return plan.stages[plan.stages.length - 1];
}

export function computeAssignmentMetrics(assignment, plans, sackWeightKg = 40) {
  const weightPerSack = Math.max(1, Number(sackWeightKg) || 40);
  const plan =
    plans.find((p) => p.id === assignment.planId) ||
    plans.find((p) => p.name === assignment.plan) ||
    plans[0];

  const liveBirds = Number(assignment.liveBirds) || 0;
  const ageDays = Number(assignment.ageDays) || 1;
  const matchedStage = findStageForAge(plan, ageDays);

  const stageLabel = assignment.stageOverrideLabel || matchedStage.name;
  const rationKg =
    assignment.rationOverrideKg != null
      ? Number(assignment.rationOverrideKg)
      : Number(matchedStage.rationKg) || 0.045;

  // Demanda diaria (kg) = aves vivas × ración por ave
  const estimatedDemandKg = liveBirds * rationKg;
  const estimatedSacks = estimatedDemandKg / weightPerSack;

  // Consumo registrado en campo vs estimado por el plan
  const registeredConsumptionKg =
    assignment.registeredConsumptionKg != null
      ? Number(assignment.registeredConsumptionKg)
      : estimatedDemandKg;
  const registeredSacks = registeredConsumptionKg / weightPerSack;
  const diffKg = registeredConsumptionKg - estimatedDemandKg;

  return {
    ...assignment,
    planId: plan?.id || assignment.planId,
    planName: plan ? `${plan.name}${plan.version > 1 ? ` v${plan.version}` : ''}` : assignment.plan || 'Sin plan',
    planVersion: plan?.version || 1,
    planClassification: plan?.badge || 'Recomendado',
    currentStageName: stageLabel,
    feedType: matchedStage.feedType || 'Alimento Balanceado',
    rationKg,
    estimatedDemandKg,
    estimatedSacks,
    registeredConsumptionKg,
    registeredSacks,
    diffKg,
    ageWeeksText: formatAgeWeeks(ageDays),
    hasProrroga: Boolean(
      assignment.hasProrroga ||
        (plan && plan.badge === 'Contingencia') ||
        String(stageLabel).toLowerCase().includes('extendido') ||
        String(stageLabel).toLowerCase().includes('prórroga')
    )
  };
}

export function computePlanSummary(stages = []) {
  if (!Array.isArray(stages) || stages.length === 0) {
    return {
      stagesCount: 0,
      firstDay: 0,
      totalCycleDays: 0,
      avgRationKg: 0,
      totalFeedPerBirdKg: 0
    };
  }

  let totalDays = 0;
  let totalFeedPerBirdKg = 0;
  let minStart = Infinity;
  let maxEnd = 0;

  stages.forEach((st) => {
    const start = Number(st.startDay) || 0;
    const end = Number(st.endDay) || 0;
    const r = Number(st.rationKg) || 0;
    if (start < minStart) minStart = start;
    if (end > maxEnd) maxEnd = end;
    const duration = Math.max(0, end - start + 1);
    totalDays += duration;
    totalFeedPerBirdKg += duration * r;
  });

  const avgRationKg = totalDays > 0 ? totalFeedPerBirdKg / totalDays : 0;

  return {
    stagesCount: stages.length,
    firstDay: minStart === Infinity ? 1 : minStart,
    totalCycleDays: maxEnd,
    activeDurationDays: totalDays,
    avgRationKg,
    totalFeedPerBirdKg
  };
}

export function validatePlanData(planForm) {
  const errors = [];

  if (!planForm.name || !planForm.name.trim()) {
    errors.push('El nombre del plan nutricional es obligatorio.');
  }
  if (!planForm.description || !planForm.description.trim()) {
    errors.push('La descripción del plan es obligatoria.');
  }

  const stages = planForm.stages || [];
  if (stages.length === 0) {
    errors.push('El plan debe tener al menos una etapa configurada.');
  }

  stages.forEach((st, idx) => {
    const label = st.name?.trim() || `Etapa ${idx + 1}`;
    const start = Number(st.startDay);
    const end = Number(st.endDay);
    const ration = Number(st.rationKg);

    if (!st.name || !st.name.trim()) {
      errors.push(`La etapa #${idx + 1} requiere un nombre.`);
    }
    if (!st.feedType || !st.feedType.trim()) {
      errors.push(`La etapa "${label}" requiere especificar el tipo de alimento.`);
    }
    if (!Number.isInteger(start) || start < 1) {
      errors.push(`En "${label}", el día inicial debe ser un entero positivo (≥ 1).`);
    }
    if (!Number.isInteger(end) || end < start) {
      errors.push(
        `En "${label}", el día final (${end}) debe ser mayor o igual al día inicial (${start}).`
      );
    }
    if (Number.isNaN(ration) || ration <= 0) {
      errors.push(
        `En "${label}", la ración diaria debe ser un número positivo mayor a 0 kg/ave/día.`
      );
    }

    if (idx === 0 && start !== 1) {
      errors.push(`La primera etapa ("${label}") debe comenzar en el Día 1 (actual: Día ${start}).`);
    }

    if (idx > 0) {
      const prev = stages[idx - 1];
      const prevEnd = Number(prev.endDay);
      const prevLabel = prev.name?.trim() || `Etapa ${idx}`;
      if (start <= prevEnd) {
        errors.push(
          `Superposición de días entre "${prevLabel}" (termina día ${prevEnd}) y "${label}" (inicia día ${start}).`
        );
      } else if (start > prevEnd + 1) {
        errors.push(
          `Existen días vacíos sin cubrir entre "${prevLabel}" (termina día ${prevEnd}) y "${label}" (inicia día ${start}). Debe iniciar el día ${prevEnd + 1}.`
        );
      }
    }
  });

  return {
    valid: errors.length === 0,
    errors,
    summary: computePlanSummary(stages)
  };
}
