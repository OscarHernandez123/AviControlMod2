import { useEffect, useState } from 'react';
import { getBarn, listAllBarns } from '../services/barnsApi';
import { getBarnNutritionPlan } from '../services/nutritionCatalogApi';

export function useNutritionBarns() {
  const [barns, setBarns] = useState([]);
  const [selectedBarnId, setSelectedBarnId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    listAllBarns({ signal: controller.signal }).then(async (listing) => {
      const details = await Promise.allSettled(listing.map((barn) => getBarn(barn.id, { signal: controller.signal })));
      const plans = await Promise.allSettled(listing.map((barn) => getBarnNutritionPlan(barn.id, { signal: controller.signal })));
      const available = listing.map((barn, index) => ({
        ...barn,
        loteActual: details[index].status === 'fulfilled' ? details[index].value.loteActual : null,
        plan: plans[index].status === 'fulfilled' ? plans[index].value : null
      }));
      if (!controller.signal.aborted) {
        setBarns(available);
        setSelectedBarnId((current) => available.some((barn) => barn.id === current)
          ? current : available.find((barn) => barn.plan?.estado === 'ACTIVO')?.id || available[0]?.id || '');
      }
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [reloadKey]);

  return {
    barns, selectedBarnId, setSelectedBarnId,
    selectedBarn: barns.find((barn) => barn.id === selectedBarnId) || null,
    loading, error, refresh: () => setReloadKey((value) => value + 1)
  };
}
