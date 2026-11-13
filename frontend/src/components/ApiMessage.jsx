function ApiMessage({ message, type = 'info', onClose }) {
  if (!message) return null;

  return (
    <div className={`avi-toast avi-toast-${type}`}>
      <span>{message}</span>
      {onClose && (
        <button type="button" className="avi-toast-close" onClick={onClose}>
          ✕
        </button>
      )}
    </div>
  );
}

export default ApiMessage;
