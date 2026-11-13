export const initialFarmState = {
  // Datos de Administrador Inicio (Node 104:23 / 216:2384)
  adminMetrics: {
    galponesTotal: 12,
    galponesEnProduccion: 10,
    inventarioCobertura: 84,
    ordenesSacrificio: 6,
    ordenesPendientes: 2
  },
  barnStatusSummary: [
    { id: 'disponible', label: 'Disponible', count: 10, widthPx: 400, color: '#59bf63' },
    { id: 'cosecha', label: 'En cosecha', count: 2, widthPx: 98, color: '#ffa80d' },
    { id: 'aislado', label: 'Aislado', count: 0, widthPx: 0, color: '#ff0d11' }
  ],
  criticalInventory: [
    {
      id: 1,
      name: 'Alimento iniciador',
      icon: '/assets/admin-feed.svg',
      tileBg: '#e6f7e4',
      status: 'Disponible',
      badgeBg: '#e6f7e4',
      badgeColor: '#2d743b',
      dotIcon: '/assets/dot-green.svg'
    },
    {
      id: 2,
      name: 'Vacuna Newcastle',
      icon: '/assets/admin-vaccine.svg',
      tileBg: '#f4eee3',
      status: 'Stock bajo',
      badgeBg: '#fff1d6',
      badgeColor: '#eb6b00',
      dotIcon: '/assets/dot-orange.svg'
    },
    {
      id: 3,
      name: 'Desinfectante',
      icon: '/assets/admin-disinfectant.svg',
      tileBg: '#f4eee3',
      status: 'Sin stock',
      badgeBg: '#ffe0e3',
      badgeColor: '#ed2b33',
      dotIcon: '/assets/dot-red.svg'
    }
  ],
  recentFeedReceptions: [
    { id: '#LDA-0012', alimento: 'Alimento A', bultos: 40, precio: '$40000', peso: '40kg', fecha: '18-09-2026' },
    { id: '#LDA-0011', alimento: 'Alimento B', bultos: 10, precio: '$10000', peso: '10kg', fecha: '19-09-2026' },
    { id: '#LDA-0010', alimento: 'Alimento C', bultos: 20, precio: '$20000', peso: '20kg', fecha: '20-09-2026' },
    { id: '#LDA-0009', alimento: 'Alimento D', bultos: 30, precio: '$30000', peso: '30kg', fecha: '21-09-2026' },
    { id: '#LDA-0008', alimento: 'Alimento E', bultos: 15, precio: '$15000', peso: '15kg', fecha: '22-09-2026' }
  ],

  // Datos de Administrador Galpones (Node 216:415 & Sidebar 216:1101)
  adminBarns: [
    {
      id: 'galpon-1',
      name: 'Galpón 1',
      lote: '#LDP-001',
      estado: 'Disponible',
      badgeBg: '#d6f0d1',
      badgeColor: '#1f5929',
      dotColor: '#2d9f42',
      aforoMaximo: '1,200 aves',
      poblacionActual: '1,000 aves',
      edadDias: '22 días',
      desgloseEdad: '3 semanas y 1 día',
      fechaIngreso: '26/08/2026'
    },
    {
      id: 'galpon-2',
      name: 'Galpón 2',
      lote: '#LDP-002',
      estado: 'Vaciado Sanitario',
      badgeBg: '#ffe8ba',
      badgeColor: '#bf5400',
      dotColor: '#f59e0b',
      aforoMaximo: '1,200 aves',
      poblacionActual: '0 aves',
      edadDias: '48 días',
      desgloseEdad: '6 semanas y 6 días',
      fechaIngreso: '01/08/2026'
    },
    {
      id: 'galpon-3',
      name: 'Galpón 3',
      lote: '#LDP-003',
      estado: 'En Cosecha',
      badgeBg: '#d1ebf0',
      badgeColor: '#1f3659',
      dotColor: '#2c5282',
      aforoMaximo: '1,500 aves',
      poblacionActual: '1,450 aves',
      edadDias: '45 días',
      desgloseEdad: '6 semanas y 3 días',
      fechaIngreso: '04/08/2026'
    },
    {
      id: 'galpon-4',
      name: 'Galpón 4',
      lote: '#LDP-004',
      estado: 'Disponible',
      badgeBg: '#d6f0d1',
      badgeColor: '#1f5929',
      dotColor: '#2d9f42',
      aforoMaximo: '1,200 aves',
      poblacionActual: '1,100 aves',
      edadDias: '18 días',
      desgloseEdad: '2 semanas y 4 días',
      fechaIngreso: '30/08/2026'
    },
    {
      id: 'galpon-5',
      name: 'Galpón 5',
      lote: '#LDP-005',
      estado: 'Aislamiento',
      badgeBg: '#ffbaba',
      badgeColor: '#bf0000',
      dotColor: '#dc2626',
      aforoMaximo: '1,200 aves',
      poblacionActual: '950 aves',
      edadDias: '31 días',
      desgloseEdad: '4 semanas y 3 días',
      fechaIngreso: '17/08/2026'
    }
  ],

  // Datos de Administrador Inventario (Node 216:3318 & Modal 386:302)
  feedInventoryKpis: {
    preInicio: '1,600 kg',
    inicio: '325 kg',
    broiler: '1,300 kg',
    totalDisponible: '3,225 kg'
  },
  feedInventory: [
    {
      id: 'inv-feed-1',
      etapa: 'Pre-inicio',
      alimento: 'Alimento A',
      cantidad: '40 bultos',
      pesoPorBulto: '40 kg',
      stockActual: '1.600 kg',
      demandaStatus: 'Cumple',
      demandaKg: '1.200 kg',
      badgeBg: '#d6f0d1',
      badgeColor: '#1f5929',
      dotColor: '#2d9f42'
    },
    {
      id: 'inv-feed-2',
      etapa: 'Inicio',
      alimento: 'Alimento B',
      cantidad: '10 bultos',
      pesoPorBulto: '10 kg',
      stockActual: '100 kg',
      demandaStatus: 'Cobertura Parcial',
      demandaKg: '200 kg',
      badgeBg: '#fff1d6',
      badgeColor: '#eb6b00',
      dotColor: '#f59e0b'
    },
    {
      id: 'inv-feed-3',
      etapa: 'Inicio',
      alimento: 'Alimento E',
      cantidad: '15 bultos',
      pesoPorBulto: '15 kg',
      stockActual: '225 kg',
      demandaStatus: 'Sin Cobertura',
      demandaKg: '500 kg',
      badgeBg: '#ffe0e3',
      badgeColor: '#ed2b33',
      dotColor: '#ef4444'
    },
    {
      id: 'inv-feed-4',
      etapa: 'Broiler',
      alimento: 'Alimento C',
      cantidad: '20 bultos',
      pesoPorBulto: '20 kg',
      stockActual: '400 kg',
      demandaStatus: 'Sin Requerimiento',
      demandaKg: '0 kg',
      badgeBg: '#dbdbdb',
      badgeColor: '#5a5a5a',
      dotColor: '#737373'
    },
    {
      id: 'inv-feed-5',
      etapa: 'Broiler',
      alimento: 'Alimento D',
      cantidad: '30 bultos',
      pesoPorBulto: '30 kg',
      stockActual: '900 kg',
      demandaStatus: 'Cumple',
      demandaKg: '750 kg',
      badgeBg: '#d6f0d1',
      badgeColor: '#1f5929',
      dotColor: '#2d9f42'
    }
  ],
  medInventoryKpis: {
    frascos: '10',
    bolsas: '5',
    cajas: '15',
    totalPresentaciones: '30'
  },
  medInventory: [
    { id: 'inv-med-1', medicamento: 'Amoxicilina', presentacion: 'Frasco', cantidad: '20', contenido: '100ml', stockActual: '2000 ml' },
    { id: 'inv-med-2', medicamento: 'Oxitetracilina', presentacion: 'Bolsa', cantidad: '12', contenido: '500 gr', stockActual: '6.000 gr' },
    { id: 'inv-med-3', medicamento: 'Antiparasito', presentacion: 'Caja', cantidad: '10', contenido: '20 unidades', stockActual: '200 unidades' },
    { id: 'inv-med-4', medicamento: 'Vitamina aviar', presentacion: 'Caja', cantidad: '5', contenido: '20 unidades', stockActual: '100 unidades' },
    { id: 'inv-med-5', medicamento: 'Enrofloxacina', presentacion: 'Frasco', cantidad: '8', contenido: '250 ml', stockActual: '2.000 ml' }
  ],

  // Datos de Administrador Recepciones Alimento (Node 216:2923 & Modals 232:4255, 379:66, 379:147)
  feedReceptions: [
    {
      id: '#LDA-0012',
      codigoLote: 'LDA-0012',
      alimento: 'Alimento A',
      bultos: 40,
      precioNeto: '$40000',
      pesoPorBulto: '40kg',
      fechaIngreso: '18-09-2026',
      fechaVencimiento: '26-10-2026',
      impuesto: '5%',
      pesoTotal: '1.600 kg',
      tipoAlimento: 'Pre-inicio',
      valorImpuesto: '$ 80.000',
      totalCompra: '$ 1.680.000',
      estado: 'Activo'
    },
    {
      id: '#LDA-0011',
      codigoLote: 'LDA-0011',
      alimento: 'Alimento B',
      bultos: 10,
      precioNeto: '$10000',
      pesoPorBulto: '10kg',
      fechaIngreso: '19-09-2026',
      fechaVencimiento: '26-10-2026',
      impuesto: '5%',
      pesoTotal: '100 kg',
      tipoAlimento: 'Alimento iniciador',
      valorImpuesto: '$ 5.000',
      totalCompra: '$ 105.000',
      estado: 'Activo'
    },
    {
      id: '#LDA-0010',
      codigoLote: 'LDA-0010',
      alimento: 'Alimento C',
      bultos: 20,
      precioNeto: '$20000',
      pesoPorBulto: '20kg',
      fechaIngreso: '20-09-2026',
      fechaVencimiento: '28-10-2026',
      impuesto: '5%',
      pesoTotal: '400 kg',
      tipoAlimento: 'Broiler',
      valorImpuesto: '$ 20.000',
      totalCompra: '$ 420.000',
      estado: 'Activo'
    },
    {
      id: '#LDA-0009',
      codigoLote: 'LDA-0009',
      alimento: 'Alimento D',
      bultos: 30,
      precioNeto: '$30000',
      pesoPorBulto: '30kg',
      fechaIngreso: '21-09-2026',
      fechaVencimiento: '30-10-2026',
      impuesto: '5%',
      pesoTotal: '900 kg',
      tipoAlimento: 'Broiler',
      valorImpuesto: '$ 45.000',
      totalCompra: '$ 945.000',
      estado: 'Activo'
    },
    {
      id: '#LDA-0008',
      codigoLote: 'LDA-0008',
      alimento: 'Alimento E',
      bultos: 15,
      precioNeto: '$15000',
      pesoPorBulto: '15kg',
      fechaIngreso: '22-09-2026',
      fechaVencimiento: '26-10-2026',
      impuesto: '5%',
      pesoTotal: '225 kg',
      tipoAlimento: 'Alimento iniciador',
      valorImpuesto: '$ 11.250',
      totalCompra: '$ 236.250',
      estado: 'Activo'
    }
  ],

  // Datos de Administrador Recepciones Medicamento (Node 216:3119 & Modals 232:3539, 383:60, 383:199)
  medReceptions: [
    {
      id: '#LDM-0012',
      codigoLote: 'LDM-0012',
      medicamento: 'Amoxicilina',
      presentacion: 'Frasco',
      cantidad: 20,
      precioNeto: '$35.000',
      contenido: '100ml',
      contenidoValor: '100',
      unidadMedida: 'ml',
      impuesto: '5%',
      fechaIngreso: '18-09-2026',
      fechaVencimiento: '26-10-2026',
      subtotalNeto: '$ 700.000',
      precioTotal: '$ 735.000'
    },
    {
      id: '#LDM-0011',
      codigoLote: 'LDM-0011',
      medicamento: 'Oxitetraciclina',
      presentacion: 'Bolsa',
      cantidad: 12,
      precioNeto: '$40.000',
      contenido: '500gr',
      contenidoValor: '500',
      unidadMedida: 'gr',
      impuesto: '5%',
      fechaIngreso: '19-09-2026',
      fechaVencimiento: '26-10-2026',
      subtotalNeto: '$ 480.000',
      precioTotal: '$ 504.000'
    },
    {
      id: '#LDM-0010',
      codigoLote: 'LDM-0010',
      medicamento: 'Antiparasitario',
      presentacion: 'Caja',
      cantidad: 10,
      precioNeto: '$28.000',
      contenido: '20uni',
      contenidoValor: '20',
      unidadMedida: 'uni',
      impuesto: '5%',
      fechaIngreso: '20-09-2026',
      fechaVencimiento: '26-10-2026',
      subtotalNeto: '$ 280.000',
      precioTotal: '$ 294.000'
    },
    {
      id: '#LDM-0009',
      codigoLote: 'LDM-0009',
      medicamento: 'Vitamina aviar',
      presentacion: 'Caja',
      cantidad: 5,
      precioNeto: '$20.000',
      contenido: '20uni',
      contenidoValor: '20',
      unidadMedida: 'uni',
      impuesto: '5%',
      fechaIngreso: '21-09-2026',
      fechaVencimiento: '26-10-2026',
      subtotalNeto: '$ 100.000',
      precioTotal: '$ 105.000'
    },
    {
      id: '#LDM-0008',
      codigoLote: 'LDM-0008',
      medicamento: 'Enrofloxacina',
      presentacion: 'Frasco',
      cantidad: 8,
      precioNeto: '$50.000',
      contenido: '250ml',
      contenidoValor: '250',
      unidadMedida: 'ml',
      impuesto: '5%',
      fechaIngreso: '22-09-2026',
      fechaVencimiento: '26-10-2026',
      subtotalNeto: '$ 400.000',
      precioTotal: '$ 420.000'
    }
  ],

  // Datos de Administrador Órdenes de Sacrificio (Node 232:3892 & Modals 232:4079, 232:4127, 232:4175)
  slaughterOrders: [
    {
      id: '#SAC-0015',
      galpon: 'Galpón 1',
      lote: '#LDP-001',
      loteColor: '#9333ea',
      edadDias: 46,
      apto: false,
      poblacion: '4.500 aves',
      fechaSacrificio: '22 Sep 2026, 06:00',
      fechaIso: '2026-09-22T06:00',
      plantaDestino: 'Planta Central Avícola - Turno Mañana',
      observaciones: 'Transporte programado con jaulas limpias y desinfectadas a las 05:30 AM.',
      estado: 'Programada',
      cantidadFinal: '4.480',
      pesoTotalKg: '11.200,00'
    },
    {
      id: '#SAC-0014',
      galpon: 'Galpón 2',
      lote: '#LDP-002',
      loteColor: '#b45309',
      edadDias: 48,
      apto: false,
      poblacion: '4.200 aves',
      fechaSacrificio: '18 Sep 2026, 09:30',
      fechaIso: '2026-09-18T09:30',
      plantaDestino: 'Planta Central Avícola - Turno Mañana',
      observaciones: 'Recepción completada sin novedades.',
      estado: 'Ejecutada',
      cantidadFinal: '4.180',
      pesoTotalKg: '10.450,00'
    },
    {
      id: 'Pendiente',
      galpon: 'Galpón 3',
      lote: '#LDP-003',
      loteColor: '#047857',
      edadDias: 45,
      apto: true,
      poblacion: '4.800 aves',
      fechaSacrificio: 'Sin programar',
      fechaIso: '2026-09-22T06:00',
      plantaDestino: 'Planta Central Avícola - Turno Mañana',
      observaciones: 'Transporte programado con jaulas limpias y desinfectadas a las 05:30 AM.',
      estado: 'En Cosecha',
      cantidadFinal: '4.800',
      pesoTotalKg: '12.000,00'
    },
    {
      id: '#SAC-0013',
      galpon: 'Galpón 4',
      lote: '#LDP-004',
      loteColor: '#a3a3a3',
      edadDias: 44,
      apto: false,
      poblacion: '4.000 aves',
      fechaSacrificio: '17 Sep 2026',
      fechaIso: '2026-09-17T08:00',
      plantaDestino: 'Planta Sur - Turno Tarde',
      observaciones: 'Orden cancelada por reprogramación de lote.',
      estado: 'Cancelada',
      cantidadFinal: '0',
      pesoTotalKg: '0,00'
    }
  ],
  evaluableBarnsForSlaughter: [
    {
      id: 'eval-3',
      galpon: 'Galpón 3',
      lote: '#LDP-003',
      edadDias: 45,
      estadoOperativo: 'Productivo',
      poblacion: '4.800 aves',
      apto: true
    },
    {
      id: 'eval-5',
      galpon: 'Galpón 5',
      lote: '#LDP-005',
      edadDias: 46,
      estadoOperativo: 'Productivo',
      poblacion: '4.100 aves',
      apto: true
    },
    {
      id: 'eval-6',
      galpon: 'Galpón 6',
      lote: '#LDP-006',
      edadDias: 38,
      estadoOperativo: 'Productivo',
      poblacion: '4.600 aves',
      apto: false
    }
  ],

  // Configuración compartida de peso por bulto (sincronizada entre Nutricionista y Trabajador)
  sackWeightKg: 40,

  // Datos de Nutricionista (con valores numéricos reales para cálculo, validación y versionado)
  nutriPlans: [
    {
      id: 'plan-1',
      name: 'Plan Estándar Broiler',
      version: 1,
      badge: 'Recomendado',
      status: 'activo',
      description: 'Optimizado para galpones comerciales estándar (Ciclo 45 días).',
      type: 'standard',
      stages: [
        {
          id: 'st-101',
          name: 'Pre-Inicio',
          startDay: 1,
          endDay: 7,
          feedType: 'Alimento A (Pre-iniciador)',
          rationKg: 0.015
        },
        {
          id: 'st-102',
          name: 'Inicio',
          startDay: 8,
          endDay: 21,
          feedType: 'Alimento B (Iniciador)',
          rationKg: 0.045
        },
        {
          id: 'st-103',
          name: 'Broiler / Engorde',
          startDay: 22,
          endDay: 45,
          feedType: 'Alimento C (Broiler)',
          rationKg: 0.090
        }
      ]
    },
    {
      id: 'plan-2',
      name: 'Crecimiento Rápido',
      version: 1,
      badge: 'Alto Rendimiento',
      status: 'activo',
      description: 'Lotes de alta densidad proteica y salida acelerada (42 días).',
      type: 'standard',
      stages: [
        {
          id: 'st-201',
          name: 'Pre-Inicio',
          startDay: 1,
          endDay: 5,
          feedType: 'Alimento A (Pre-iniciador)',
          rationKg: 0.018
        },
        {
          id: 'st-202',
          name: 'Inicio',
          startDay: 6,
          endDay: 18,
          feedType: 'Alimento E (Iniciador Plus)',
          rationKg: 0.052
        },
        {
          id: 'st-203',
          name: 'Broiler / Engorde',
          startDay: 19,
          endDay: 42,
          feedType: 'Alimento D (Broiler Alta Densidad)',
          rationKg: 0.098
        }
      ]
    },
    {
      id: 'plan-3',
      name: 'Sanitario / Prórroga',
      version: 1,
      badge: 'Contingencia',
      status: 'activo',
      description: 'Diseñado para galpones en recuperación o aislamiento (50 días).',
      type: 'contingency',
      stages: [
        {
          id: 'st-301',
          name: 'Pre-Inicio',
          startDay: 1,
          endDay: 7,
          feedType: 'Alimento A (Pre-iniciador)',
          rationKg: 0.015
        },
        {
          id: 'st-302',
          name: 'Inicio Extendido',
          startDay: 8,
          endDay: 26,
          extendedDays: 5,
          feedType: 'Alimento B (Iniciador Medicado)',
          rationKg: 0.040
        },
        {
          id: 'st-303',
          name: 'Engorde Final',
          startDay: 27,
          endDay: 50,
          feedType: 'Alimento C (Broiler)',
          rationKg: 0.085
        }
      ]
    }
  ],
  assignedBarnPlans: [
    {
      id: 'asg-1',
      galpon: 'Galpón 1',
      loteId: '#LDP-001',
      liveBirds: 1200,
      ageDays: 25,
      planId: 'plan-1',
      startDate: '2026-10-12',
      registeredConsumptionKg: 108.0,
      hasProrroga: false,
      stageOverrideLabel: null,
      rationOverrideKg: null,
      notes: 'Lote en crecimiento normal conforme a curva estándar.'
    },
    {
      id: 'asg-2',
      galpon: 'Galpón 2',
      loteId: '#LDP-002',
      liveBirds: 850,
      ageDays: 16,
      planId: 'plan-3',
      startDate: '2026-10-21',
      registeredConsumptionKg: 35.2,
      hasProrroga: true,
      stageOverrideLabel: 'Inicio Extendido (+5d)',
      rationOverrideKg: null,
      notes: 'Prórroga de 5 días en etapa de inicio por bajo peso en semana 2.'
    },
    {
      id: 'asg-3',
      galpon: 'Galpón 3',
      loteId: '#LDP-003',
      liveBirds: 1450,
      ageDays: 12,
      planId: 'plan-2',
      startDate: '2026-10-25',
      registeredConsumptionKg: 75.4,
      hasProrroga: false,
      stageOverrideLabel: null,
      rationOverrideKg: null,
      notes: 'Régimen de alto rendimiento proteico.'
    },
    {
      id: 'asg-4',
      galpon: 'Galpón 4',
      loteId: '#LDP-004',
      liveBirds: 5200,
      ageDays: 34,
      planId: 'plan-1',
      startDate: '2026-10-03',
      registeredConsumptionKg: 465.0,
      hasProrroga: false,
      stageOverrideLabel: null,
      rationOverrideKg: null,
      notes: 'Lote comercial en fase final de engorde.'
    },
    {
      id: 'asg-5',
      galpon: 'Galpón 5',
      loteId: '#LDP-005',
      liveBirds: 8218,
      ageDays: 38,
      planId: 'plan-1',
      startDate: '2026-09-29',
      registeredConsumptionKg: 735.0,
      hasProrroga: false,
      stageOverrideLabel: null,
      rationOverrideKg: null,
      notes: 'Monitoreo preventivo solicitado por operario.'
    }
  ],

  // Datos de Veterinario Inicio y Módulos Clínicos (Node 106:60)
  vetDiagnosedBarns: [
    { id: 1, name: 'Galpón 7', status: 'Reintegro', badgeBg: '#e6f7e4', badgeColor: '#2d743b', dotIcon: '/assets/vet-dot-reintegro1.svg' },
    { id: 2, name: 'Galpón 6', status: 'Reintegro', badgeBg: '#e6f7e4', badgeColor: '#2d743b', dotIcon: '/assets/vet-dot-reintegro2.svg' },
    { id: 3, name: 'Galpón 9', status: 'Sacrificio', badgeBg: '#ffe0e3', badgeColor: '#ed2b33', dotIcon: '/assets/dot-red.svg' }
  ],
  vetReviewRequests: [
    { id: '#0001', galpon: 'Galpón 4', lote: '#LDP-004', motivo: 'Jadeos respiratorios leves en sector norte', fecha: '12 sep. 2026', estado: 'Aceptada', badgeBg: '#d6f0d1', badgeColor: '#1f5929', dotIcon: '/assets/vet-dot-aceptada.svg', widthPx: 107 },
    { id: '#0002', galpon: 'Galpón 2', lote: '#LDP-002', motivo: 'Decaimiento y baja ingesta de agua en las últimas 24h', fecha: '15 sep. 2026', estado: 'Pendiente', badgeBg: '#ffe8ba', badgeColor: '#bf5400', dotIcon: '/assets/vet-dot-pendiente.svg', widthPx: 107 },
    { id: '#0003', galpon: 'Galpón 1', lote: '#LDP-001', motivo: 'Revisión preventiva solicitada por cambio de cama', fecha: '10 sep. 2026', estado: 'Rechazada', badgeBg: '#f0d1d2', badgeColor: '#591f20', dotIcon: '/assets/vet-dot-rechazada.svg', widthPx: 115 },
    { id: '#0004', galpon: 'Galpón 3', lote: '#LDP-003', motivo: 'Control post-vacunación de refuerzo', fecha: '08 sep. 2026', estado: 'Aceptada', badgeBg: '#d6f0d1', badgeColor: '#1f5929', dotIcon: '/assets/vet-dot-aceptada.svg', widthPx: 107 },
    { id: '#0005', galpon: 'Galpón 5', lote: '#LDP-005', motivo: 'Aislamiento preventivo solicitado por sintomatología entérica', fecha: '18 sep. 2026', estado: 'Pendiente', badgeBg: '#ffe8ba', badgeColor: '#bf5400', dotIcon: '/assets/vet-dot-pendiente.svg', widthPx: 107 }
  ],
  vetDiseases: [
    {
      id: '#ENF-001',
      nombre: 'Enfermedad de Newcastle',
      tipo: 'Viral',
      agente: 'Paramixovirus aviar tipo 1',
      riesgo: 'Alto',
      aislamientoObligatorio: true,
      sintomas: 'Disnea severa, estertores traqueales, tortícolis y caída brusca de consumo.',
      protocolo: 'Cuarentena estricta del galpón, pediluvios con amonio cuaternario y notificación sanitaria.'
    },
    {
      id: '#ENF-002',
      nombre: 'Bronquitis Infecciosa Aviar',
      tipo: 'Viral',
      agente: 'Coronavirus aviar (IBV)',
      riesgo: 'Alto',
      aislamientoObligatorio: true,
      sintomas: 'Tos, estornudos, exudado nasal seroso y depresión en aves jóvenes.',
      protocolo: 'Aislamiento térmico, terapia electrolítica de soporte y refuerzo de ventilación.'
    },
    {
      id: '#ENF-003',
      nombre: 'Colibacilosis Aviar',
      tipo: 'Bacteriana',
      agente: 'Escherichia coli patógena aviar (APEC)',
      riesgo: 'Medio',
      aislamientoObligatorio: false,
      sintomas: 'Diarrea acuosa, plumas erizadas, aerosaculitis y retraso en ganancia de peso.',
      protocolo: 'Antibioticoterapia en agua de bebida durante 5 días y cloración de líneas de agua.'
    },
    {
      id: '#ENF-004',
      nombre: 'Coccidiosis Intestinal',
      tipo: 'Parasitaria',
      agente: 'Eimeria acervulina / Eimeria tenella',
      riesgo: 'Medio',
      aislamientoObligatorio: false,
      sintomas: 'Heces sanguinolentas o mucosas, palidez de cresta y deshidratación.',
      protocolo: 'Tratamiento anticoccidial inmediato en agua y recambio de cama húmeda.'
    },
    {
      id: '#ENF-005',
      nombre: 'Micoplasmosis Respiratoria (ERC)',
      tipo: 'Bacteriana',
      agente: 'Mycoplasma gallisepticum',
      riesgo: 'Alto',
      aislamientoObligatorio: true,
      sintomas: 'Sinusitis infraorbitaria, descarga ocular espumosa y ruidos respiratorios nocturnos.',
      protocolo: 'Aislamiento del lote afectado y medicación con macrólidos o fluoroquinolonas.'
    }
  ],
  vetMedicines: [
    {
      id: '#MED-001',
      nombre: 'Enrofloxacina 10% Solución Oral',
      categoria: 'Antibiótico',
      principioActivo: 'Enrofloxacina 100 mg/ml',
      via: 'Agua de bebida',
      dosisRecomendada: '10 mg/kg PV (1 ml por cada 2 L de agua) por 3 a 5 días',
      tiempoRetiroDias: 7,
      indicaciones: 'Infecciones respiratorias y entéricas por E. coli, Mycoplasma y Pasteurella.'
    },
    {
      id: '#MED-002',
      nombre: 'Amoxicilina Trihidrato 50%',
      categoria: 'Antibiótico',
      principioActivo: 'Amoxicilina 500 mg/g polvo soluble',
      via: 'Agua de bebida',
      dosisRecomendada: '20 mg/kg PV (40 g por cada 200 L de agua) por 5 días',
      tiempoRetiroDias: 5,
      indicaciones: 'Colibacilosis, enteritis necrótica y procesos bacterianos secundarios.'
    },
    {
      id: '#MED-003',
      nombre: 'Toltrazuril 2.5% Anticoccidial',
      categoria: 'Antiparasitario',
      principioActivo: 'Toltrazuril 25 mg/ml',
      via: 'Agua de bebida',
      dosisRecomendada: '7 mg/kg PV (1 L por cada 1.000 L de agua) durante 2 días continuos',
      tiempoRetiroDias: 14,
      indicaciones: 'Prevención y control clínico de coccidiosis intestinal y cecal.'
    },
    {
      id: '#MED-004',
      nombre: 'Vacuna viva Newcastle Cepa LaSota',
      categoria: 'Vacuna',
      principioActivo: 'Virus vivo atenuado NDV cepa LaSota',
      via: 'Ocular / Agua',
      dosisRecomendada: '1 dosis por ave en agua desclorada o gota ocular',
      tiempoRetiroDias: 0,
      indicaciones: 'Inmunización activa de lotes comerciales contra Enfermedad de Newcastle.'
    },
    {
      id: '#MED-005',
      nombre: 'Complejo Electrolítico + Vitamina C',
      categoria: 'Vitamina / Electrolito',
      principioActivo: 'Sales rehidratantes + Ácido ascórbico + Vitamina A/D3/E',
      via: 'Agua de bebida',
      dosisRecomendada: '1 g por litro de agua durante 3 a 5 días',
      tiempoRetiroDias: 0,
      indicaciones: 'Recuperación post-vacunal, estrés calórico y convalecencia infecciosa.'
    }
  ],
  vetDiagnoses: [
    {
      id: '#DIAG-001',
      solicitudId: '#0001',
      galpon: 'Galpón 4',
      lote: '#LDP-004',
      fecha: '2026-09-12',
      enfermedad: 'Colibacilosis Aviar',
      severidad: 'Moderada',
      medicina: 'Amoxicilina Trihidrato 50%',
      dosis: '40 g / 200 L agua por 5 días',
      dictamen: 'Reintegro',
      observaciones: 'Evolución favorable tras 5 días de tratamiento. Lote apto para reintegro normal.'
    },
    {
      id: '#DIAG-002',
      solicitudId: '#0004',
      galpon: 'Galpón 3',
      lote: '#LDP-003',
      fecha: '2026-09-14',
      enfermedad: 'Coccidiosis Intestinal',
      severidad: 'Leve',
      medicina: 'Toltrazuril 2.5% Anticoccidial',
      dosis: '1 ml / L de agua por 48 horas',
      dictamen: 'Reintegro',
      observaciones: 'Control parasitario completado. Respetar tiempo de retiro de 14 días.'
    },
    {
      id: '#DIAG-003',
      solicitudId: '#0005',
      galpon: 'Galpón 5',
      lote: '#LDP-005',
      fecha: '2026-09-18',
      enfermedad: 'Micoplasmosis Respiratoria (ERC)',
      severidad: 'Moderada',
      medicina: 'Enrofloxacina 10% Solución Oral',
      dosis: '1 ml / 2 L agua por 5 días',
      dictamen: 'En tratamiento',
      observaciones: 'Galpón mantenido en aislamiento preventivo con monitoreo respiratorio cada 12h.'
    },
    {
      id: '#DIAG-004',
      solicitudId: 'Directo',
      galpon: 'Galpón 9',
      lote: '#LDP-009',
      fecha: '2026-09-19',
      enfermedad: 'Enfermedad de Newcastle',
      severidad: 'Crítica',
      medicina: 'Sin tratamiento curativo (Brote agudo)',
      dosis: 'Protocolo de contención sanitaria',
      dictamen: 'Sacrificio',
      observaciones: 'Brote respiratorio severo confirmado. Se emite orden de sacrificio sanitario inmediato.'
    }
  ],
  vetSlaughterOrders: [
    {
      id: '#SAC-VET-01',
      galpon: 'Galpón 9',
      lote: '#LDP-009',
      enfermedad: 'Enfermedad de Newcastle',
      motivo: 'Contención sanitaria por brote viral de alta transmisibilidad',
      prioridad: 'Urgente',
      fechaEmision: '2026-09-19',
      protocoloBioseguridad: 'Despoblado en circuito cerrado, compostaje in situ y vacío sanitario de 21 días',
      estado: 'Pendiente ejecución'
    },
    {
      id: '#SAC-VET-02',
      galpon: 'Galpón 3',
      lote: '#LDP-003',
      enfermedad: 'Apto comercial (Retiro cumplido)',
      motivo: 'Certificación veterinaria de fin de retiro farmacológico para cosecha',
      prioridad: 'Programada',
      fechaEmision: '2026-09-17',
      protocoloBioseguridad: 'Ayuno hídrico controlado y transporte en jaulas desinfectadas',
      estado: 'Aprobada'
    },
    {
      id: '#SAC-VET-03',
      galpon: 'Galpón 2',
      lote: '#LDP-002',
      enfermedad: 'Micoplasmosis Respiratoria (ERC)',
      motivo: 'Descarte sanitario focalizado por secuelas respiratorias crónicas',
      prioridad: 'Alta',
      fechaEmision: '2026-09-11',
      protocoloBioseguridad: 'Inspección antemortem y desinfección terminal de equipos',
      estado: 'Ejecutada'
    }
  ],

  // Datos de Trabajador Inicio (Node 238:5822)
  workerBarns: [
    {
      id: 'galpon-1',
      name: 'Galpón 1',
      lote: 'Lote #LDP-001',
      loteCode: '#LDP-001',
      loteBg: '#ecfdf5',
      loteColor: '#065f46',
      status: 'En cosecha',
      estadoProduccion: 'En producción',
      poblacionNum: 5000,
      poblacion: '5.000 aves',
      edad: '38 días',
      dieta: 'Engorde Stage 2',
      racion: '450 kg (9 bultos)',
      cuotaKg: '450 kg',
      bultos: '9 bultos',
      estadoBodega: 'Suficiente',
      pendingValidation: false,
      alertCode: '',
      alertText: ''
    },
    {
      id: 'galpon-3',
      name: 'Galpón 3',
      lote: 'Lote #LDP-003',
      loteCode: '#LDP-003',
      loteBg: '#ecfdf5',
      loteColor: '#065f46',
      status: 'En cosecha',
      estadoProduccion: 'En producción',
      poblacionNum: 4800,
      poblacion: '4.800 aves',
      edad: '35 días',
      dieta: 'Engorde Stage 2',
      racion: '430 kg (8.6 bultos)',
      cuotaKg: '430 kg',
      bultos: '8.6 bultos',
      estadoBodega: 'Suficiente',
      pendingValidation: false,
      alertCode: '',
      alertText: ''
    },
    {
      id: 'galpon-5',
      name: 'Galpón 5',
      lote: 'Lote #LDP-005',
      loteCode: '#LDP-005',
      loteBg: '#faf5ff',
      loteColor: '#581c87',
      status: 'Pendiente de validación',
      estadoProduccion: 'En producción',
      poblacionNum: 4000,
      poblacion: '4.000 aves',
      edad: '41 días',
      dieta: 'Engorde Finisher',
      racion: '370 kg (7.4 bultos)',
      cuotaKg: '370 kg',
      bultos: '7.4 bultos',
      estadoBodega: 'Suficiente',
      pendingValidation: true,
      alertCode: '#SOL-004',
      alertText: 'Solicitud #SOL-004 registrada: Esperando evaluación del veterinario para aislamiento.'
    }
  ]
};

export function farmReducer(state, action) {
  switch (action.type) {
    case 'ADD_FEED_RECEPTION':
      return {
        ...state,
        feedReceptions: [action.payload, ...state.feedReceptions],
        recentFeedReceptions: [
          {
            id: action.payload.id,
            alimento: action.payload.alimento,
            bultos: action.payload.bultos,
            precio: action.payload.precioNeto,
            peso: action.payload.pesoPorBulto,
            fecha: action.payload.fechaIngreso
          },
          ...state.recentFeedReceptions
        ]
      };
    case 'UPDATE_FEED_RECEPTION':
      return {
        ...state,
        feedReceptions: state.feedReceptions.map((item) =>
          item.id === action.payload.id ? { ...item, ...action.payload } : item
        ),
        recentFeedReceptions: state.recentFeedReceptions.map((item) =>
          item.id === action.payload.id
            ? {
                ...item,
                alimento: action.payload.alimento,
                bultos: action.payload.bultos,
                precio: action.payload.precioNeto,
                peso: action.payload.pesoPorBulto
              }
            : item
        )
      };
    case 'ADD_MED_RECEPTION':
      return {
        ...state,
        medReceptions: [action.payload, ...state.medReceptions]
      };
    case 'UPDATE_MED_RECEPTION':
      return {
        ...state,
        medReceptions: state.medReceptions.map((item) =>
          item.id === action.payload.id ? { ...item, ...action.payload } : item
        )
      };
    case 'VALIDATE_BARN_FOR_SLAUGHTER': {
      const target = action.payload;
      const alreadyExists = state.slaughterOrders.some((o) => o.galpon === target.galpon);
      if (alreadyExists) {
        return {
          ...state,
          slaughterOrders: state.slaughterOrders.map((o) =>
            o.galpon === target.galpon ? { ...o, estado: 'En Cosecha', apto: true } : o
          )
        };
      }
      return {
        ...state,
        slaughterOrders: [
          {
            id: 'Pendiente',
            galpon: target.galpon,
            lote: target.lote,
            loteColor: '#047857',
            edadDias: target.edadDias,
            apto: true,
            poblacion: target.poblacion,
            fechaSacrificio: 'Sin programar',
            fechaIso: '2026-09-22T06:00',
            plantaDestino: 'Planta Central Avícola - Turno Mañana',
            observaciones: 'Transporte programado con jaulas limpias y desinfectadas a las 05:30 AM.',
            estado: 'En Cosecha',
            cantidadFinal: '4.100',
            pesoTotalKg: '10.250,00'
          },
          ...state.slaughterOrders
        ]
      };
    }
    case 'SCHEDULE_SLAUGHTER_ORDER':
      return {
        ...state,
        slaughterOrders: state.slaughterOrders.map((o) =>
          o.galpon === action.payload.galpon
            ? {
                ...o,
                id: o.id === 'Pendiente' ? `#SAC-0016` : o.id,
                fechaSacrificio: action.payload.fechaFormateada,
                fechaIso: action.payload.fechaIso,
                plantaDestino: action.payload.plantaDestino,
                observaciones: action.payload.observaciones,
                estado: 'Programada',
                apto: false
              }
            : o
        )
      };
    case 'CANCEL_SLAUGHTER_ORDER':
      return {
        ...state,
        slaughterOrders: state.slaughterOrders.map((o) =>
          o.galpon === action.payload ? { ...o, estado: 'Cancelada', loteColor: '#a3a3a3' } : o
        )
      };
    case 'SAVE_SLAUGHTER_RESULTS':
      return {
        ...state,
        slaughterOrders: state.slaughterOrders.map((o) =>
          o.galpon === action.payload.galpon
            ? {
                ...o,
                cantidadFinal: action.payload.cantidadFinal,
                pesoTotalKg: action.payload.pesoTotalKg
              }
            : o
        )
      };
    case 'REGISTER_WORKER_MORTALITY': {
      const { barnId, cantidadBajas } = action.payload;
      const loss = Math.max(0, parseInt(cantidadBajas, 10) || 0);
      const targetBarn = state.workerBarns.find((b) => b.id === barnId);
      const targetBarnName = targetBarn?.name;

      return {
        ...state,
        workerBarns: state.workerBarns.map((b) => {
          if (b.id !== barnId) return b;
          const nextNum = Math.max(0, (b.poblacionNum || 5000) - loss);
          const formatted = `${nextNum.toLocaleString('es-CO')} aves`;
          return {
            ...b,
            poblacionNum: nextNum,
            poblacion: formatted
          };
        }),
        assignedBarnPlans: state.assignedBarnPlans.map((row) => {
          if (row.galpon !== targetBarnName) return row;
          const nextLive = Math.max(0, (Number(row.liveBirds) || 0) - loss);
          return {
            ...row,
            liveBirds: nextLive
          };
        })
      };
    }
    case 'REQUEST_BARN_ISOLATION': {
      const { barnId, motivo } = action.payload;
      const nextCodeNum = state.vetReviewRequests.length + 1;
      const solCode = `#SOL-00${nextCodeNum}`;
      const targetBarn = state.workerBarns.find((b) => b.id === barnId);
      return {
        ...state,
        workerBarns: state.workerBarns.map((b) =>
          b.id === barnId
            ? {
                ...b,
                pendingValidation: true,
                status: 'Pendiente de validación',
                alertCode: solCode,
                alertText: `Solicitud ${solCode} registrada: ${motivo || 'Esperando evaluación del veterinario para aislamiento.'}`
              }
            : b
        ),
        vetReviewRequests: targetBarn
          ? [
              {
                id: `#000${nextCodeNum}`,
                galpon: targetBarn.name,
                fecha: '19 sep. 2026',
                estado: 'Pendiente',
                badgeBg: '#ffe8ba',
                badgeColor: '#bf5400',
                dotIcon: '/assets/vet-dot-pendiente.svg',
                widthPx: 107
              },
              ...state.vetReviewRequests
            ]
          : state.vetReviewRequests
      };
    }
    case 'SAVE_NUTRI_PLAN': {
      const { plan, createVersion, applyToBarns = [] } = action.payload;
      const exists = state.nutriPlans.some((p) => p.id === plan.id);

      // Si el usuario eligió crear una nueva versión de un plan ya asignado
      if (exists && createVersion) {
        const currentPlan = state.nutriPlans.find((p) => p.id === plan.id);
        const nextVersionNumber = (Number(currentPlan?.version) || 1) + 1;
        const newPlanId = `${plan.id}-v${nextVersionNumber}-${Date.now().toString().slice(-3)}`;
        const versionedPlan = {
          ...plan,
          id: newPlanId,
          version: nextVersionNumber
        };

        return {
          ...state,
          nutriPlans: [...state.nutriPlans, versionedPlan],
          assignedBarnPlans: state.assignedBarnPlans.map((row) =>
            applyToBarns.includes(row.galpon)
              ? {
                  ...row,
                  planId: newPlanId,
                  stageOverrideLabel: null,
                  rationOverrideKg: null,
                  hasProrroga: versionedPlan.badge === 'Contingencia'
                }
              : row
          )
        };
      }

      if (exists) {
        return {
          ...state,
          nutriPlans: state.nutriPlans.map((p) =>
            p.id === plan.id ? { ...p, ...plan } : p
          )
        };
      }

      return {
        ...state,
        nutriPlans: [...state.nutriPlans, plan]
      };
    }
    case 'SET_NUTRI_PLAN_STATUS': {
      const { planId, status } = action.payload;
      return {
        ...state,
        nutriPlans: state.nutriPlans.map((p) =>
          p.id === planId ? { ...p, status } : p
        )
      };
    }
    case 'ASSIGN_NUTRI_PLAN': {
      const { galpon, loteId, liveBirds, ageDays, planId, startDate, notes } =
        action.payload;
      const chosenPlan = state.nutriPlans.find((p) => p.id === planId);
      return {
        ...state,
        assignedBarnPlans: state.assignedBarnPlans.map((row) => {
          if (row.galpon !== galpon) return row;
          return {
            ...row,
            loteId: loteId || row.loteId,
            liveBirds:
              liveBirds != null ? Number(liveBirds) : Number(row.liveBirds),
            ageDays: ageDays != null ? Number(ageDays) : Number(row.ageDays),
            planId,
            startDate: startDate || row.startDate,
            notes: notes !== undefined ? notes : row.notes,
            hasProrroga: chosenPlan?.badge === 'Contingencia',
            stageOverrideLabel: null,
            rationOverrideKg: null
          };
        })
      };
    }
    case 'ADJUST_NUTRI_STAGE': {
      const { galpon, stageIndex = 1, etapaLabel, nuevoDiaFin, justificacion } =
        action.payload;
      const endDayNum = Math.max(2, parseInt(nuevoDiaFin, 10) || 26);

      const targetRow = state.assignedBarnPlans.find((r) => r.galpon === galpon);
      const targetPlanId = targetRow?.planId;

      return {
        ...state,
        assignedBarnPlans: state.assignedBarnPlans.map((row) =>
          row.galpon === galpon
            ? {
                ...row,
                hasProrroga: true,
                stageOverrideLabel: `${etapaLabel} (hasta día ${endDayNum})`,
                notes: justificacion || row.notes
              }
            : row
        ),
        nutriPlans: state.nutriPlans.map((plan) => {
          if (plan.id !== targetPlanId) return plan;
          const updatedStages = plan.stages.map((st) => ({ ...st }));
          const idx = Math.min(
            Math.max(0, Number(stageIndex) || 0),
            updatedStages.length - 1
          );
          const oldEnd = Number(updatedStages[idx].endDay) || endDayNum;
          const diff = endDayNum - oldEnd;
          updatedStages[idx] = {
            ...updatedStages[idx],
            endDay: endDayNum,
            extendedDays:
              diff > 0
                ? (Number(updatedStages[idx].extendedDays) || 0) + diff
                : updatedStages[idx].extendedDays
          };

          // Desplazar en cascada las etapas siguientes sin solapamientos ni huecos
          for (let i = idx + 1; i < updatedStages.length; i += 1) {
            const prevEnd = Number(updatedStages[i - 1].endDay);
            const origDuration = Math.max(
              1,
              Number(updatedStages[i].endDay) - Number(updatedStages[i].startDay)
            );
            updatedStages[i] = {
              ...updatedStages[i],
              startDay: prevEnd + 1,
              endDay: Math.max(prevEnd + 1 + origDuration, Number(updatedStages[i].endDay))
            };
          }

          return {
            ...plan,
            stages: updatedStages
          };
        })
      };
    }
    case 'REGISTER_BARN_FEED_CONSUMPTION': {
      const { galpon, registeredConsumptionKg } = action.payload;
      return {
        ...state,
        assignedBarnPlans: state.assignedBarnPlans.map((row) =>
          row.galpon === galpon
            ? {
                ...row,
                registeredConsumptionKg: Math.max(0, Number(registeredConsumptionKg) || 0)
              }
            : row
        )
      };
    }
    case 'SET_SACK_WEIGHT_KG': {
      const nextWeight = Math.max(1, Number(action.payload) || 40);
      return {
        ...state,
        sackWeightKg: nextWeight,
        workerBarns: state.workerBarns.map((b) => {
          const cuotaNum = parseFloat(String(b.cuotaKg).replace(/[^0-9.]/g, '')) || 450;
          const sacks = Number((cuotaNum / nextWeight).toFixed(1));
          return {
            ...b,
            bultos: `${sacks} bultos`,
            racion: `${cuotaNum} kg (${sacks} bultos)`
          };
        })
      };
    }
    case 'RESET_NUTRI_DEMO_DATA': {
      return {
        ...state,
        sackWeightKg: initialFarmState.sackWeightKg,
        nutriPlans: initialFarmState.nutriPlans,
        assignedBarnPlans: initialFarmState.assignedBarnPlans
      };
    }
    case 'SAVE_VET_DISEASE': {
      const item = action.payload;
      const exists = state.vetDiseases.some((d) => d.id === item.id);
      return {
        ...state,
        vetDiseases: exists
          ? state.vetDiseases.map((d) => (d.id === item.id ? { ...d, ...item } : d))
          : [item, ...state.vetDiseases]
      };
    }
    case 'SAVE_VET_MEDICINE': {
      const item = action.payload;
      const exists = state.vetMedicines.some((m) => m.id === item.id);
      return {
        ...state,
        vetMedicines: exists
          ? state.vetMedicines.map((m) => (m.id === item.id ? { ...m, ...item } : m))
          : [item, ...state.vetMedicines]
      };
    }
    case 'SAVE_VET_DIAGNOSIS': {
      const diag = action.payload;
      const exists = state.vetDiagnoses.some((d) => d.id === diag.id);
      const nextDiagnoses = exists
        ? state.vetDiagnoses.map((d) => (d.id === diag.id ? { ...d, ...diag } : d))
        : [diag, ...state.vetDiagnoses];

      // Actualizar estado de la solicitud vinculada (si aplica)
      const nextRequests = state.vetReviewRequests.map((req) => {
        if (req.id !== diag.solicitudId) return req;
        return {
          ...req,
          estado: 'Aceptada',
          badgeBg: '#d6f0d1',
          badgeColor: '#1f5929',
          dotIcon: '/assets/vet-dot-aceptada.svg'
        };
      });

      // Actualizar lista de galpones diagnosticados
      const statusBadge =
        diag.dictamen === 'Sacrificio'
          ? {
              status: 'Sacrificio',
              badgeBg: '#ffe0e3',
              badgeColor: '#ed2b33',
              dotIcon: '/assets/dot-red.svg'
            }
          : diag.dictamen === 'En tratamiento'
          ? {
              status: 'Aislamiento',
              badgeBg: '#ffe8ba',
              badgeColor: '#bf5400',
              dotIcon: '/assets/dot-orange.svg'
            }
          : {
              status: 'Reintegro',
              badgeBg: '#e6f7e4',
              badgeColor: '#2d743b',
              dotIcon: '/assets/vet-dot-reintegro1.svg'
            };

      const barnExists = state.vetDiagnosedBarns.some((b) => b.name === diag.galpon);
      const nextDiagnosedBarns = barnExists
        ? state.vetDiagnosedBarns.map((b) =>
            b.name === diag.galpon ? { ...b, ...statusBadge } : b
          )
        : [
            {
              id: Date.now(),
              name: diag.galpon,
              ...statusBadge
            },
            ...state.vetDiagnosedBarns
          ];

      return {
        ...state,
        vetDiagnoses: nextDiagnoses,
        vetReviewRequests: nextRequests,
        vetDiagnosedBarns: nextDiagnosedBarns
      };
    }
    case 'UPDATE_VET_REQUEST_STATUS': {
      const { id, estado } = action.payload;
      const styleMap = {
        Aceptada: {
          badgeBg: '#d6f0d1',
          badgeColor: '#1f5929',
          dotIcon: '/assets/vet-dot-aceptada.svg',
          widthPx: 107
        },
        Rechazada: {
          badgeBg: '#f0d1d2',
          badgeColor: '#591f20',
          dotIcon: '/assets/vet-dot-rechazada.svg',
          widthPx: 115
        },
        Pendiente: {
          badgeBg: '#ffe8ba',
          badgeColor: '#bf5400',
          dotIcon: '/assets/vet-dot-pendiente.svg',
          widthPx: 107
        }
      };
      const st = styleMap[estado] || styleMap.Pendiente;
      return {
        ...state,
        vetReviewRequests: state.vetReviewRequests.map((req) =>
          req.id === id ? { ...req, estado, ...st } : req
        )
      };
    }
    case 'SAVE_VET_SLAUGHTER_ORDER': {
      const order = action.payload;
      const exists = state.vetSlaughterOrders.some((o) => o.id === order.id);
      return {
        ...state,
        vetSlaughterOrders: exists
          ? state.vetSlaughterOrders.map((o) => (o.id === order.id ? { ...o, ...order } : o))
          : [order, ...state.vetSlaughterOrders]
      };
    }
    case 'UPDATE_VET_SLAUGHTER_STATUS': {
      const { id, estado } = action.payload;
      return {
        ...state,
        vetSlaughterOrders: state.vetSlaughterOrders.map((o) =>
          o.id === id ? { ...o, estado } : o
        )
      };
    }
    default:
      return state;
  }
}
