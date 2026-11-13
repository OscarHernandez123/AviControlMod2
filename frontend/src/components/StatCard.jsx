function StatCard({ label, value, status, statusColor = '#2d743b', icon, tileBg = '#f4eee3', width = '437px' }) {
  return (
    <div className="avi-metric-card" style={{ width }}>
      <div className="avi-metric-icon-tile" style={{ backgroundColor: tileBg }}>
        <div className="avi-metric-icon-inner">
          <img alt={label} src={icon} />
        </div>
      </div>
      <div className="avi-metric-content">
        <p className="avi-metric-label">{label}</p>
        <p className="avi-metric-value">{value}</p>
        <p className="avi-metric-status" style={{ color: statusColor }}>
          {status}
        </p>
      </div>
    </div>
  );
}

export default StatCard;
