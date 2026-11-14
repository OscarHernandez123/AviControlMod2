function BaseModal({ isOpen, onClose, children, maxWidth = '440px' }) {
  if (!isOpen) return null;

  return (
    <div className="avi-modal-overlay" onClick={onClose}>
      <div
        className="avi-modal-container"
        style={{ maxWidth }}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
}

export default BaseModal;
