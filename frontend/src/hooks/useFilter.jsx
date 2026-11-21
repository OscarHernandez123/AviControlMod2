import { useState, useMemo } from 'react';

export function useFilter(items = [], searchFields = []) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      const matchesSearch =
        !searchTerm ||
        searchFields.some((field) =>
          String(item[field] || '')
            .toLowerCase()
            .includes(searchTerm.toLowerCase())
        );
      const matchesStatus =
        statusFilter === 'ALL' || item.status === statusFilter || item.estado === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [items, searchTerm, statusFilter, searchFields]);

  return {
    searchTerm,
    setSearchTerm,
    statusFilter,
    setStatusFilter,
    filteredItems
  };
}
