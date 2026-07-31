import React, { useState, useEffect, useRef } from 'react';
import { User, Mail, Users, Sparkles, X, Plus } from 'lucide-react';

function AdditionalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [coApplicants, setCoApplicants] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Use a ref to track whether the update was self-triggered to prevent re-render lag
  const isSelfTriggeredRef = useRef(false);

  // Initialize and synchronize state when user changes
  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setName(user?.name || '');
    setEmail(user?.email || '');
    
    const members = user?.additionalMembers || [];
    const padded = [...members];
    while (padded.length < 3) {
      padded.push({ name: '' });
    }
    setCoApplicants(padded);
  }, [user]);

  const syncChanges = (updatedName, updatedEmail, updatedMembers) => {
    // Filter out members that have empty names for the backend/count sync
    const activeMembers = updatedMembers.filter(m => m && m.name && m.name.trim() !== '');

    const updatedUser = {
      ...user,
      name: updatedName,
      email: updatedEmail,
      extraPersonsCount: activeMembers.length,
      additionalMembers: updatedMembers
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          name: updatedName,
          email: updatedEmail
        },
        inventors: activeMembers.map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
    } else {
      updatedData = {
        applicant: {
          name: updatedName,
          email: updatedEmail
        },
        inventors: activeMembers.map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
    }

    if (user && onUserUpdate) {
      onUserUpdate(updatedUser);
    }
    if (onChange) {
      onChange(updatedData);
    }
  };

  const handleNameChange = (val) => {
    isSelfTriggeredRef.current = true;
    setName(val);
    syncChanges(val, email, coApplicants);
  };

  const handleEmailChange = (val) => {
    isSelfTriggeredRef.current = true;
    setEmail(val);
    syncChanges(name, val, coApplicants);
  };

  const handleAddMember = () => {
    isSelfTriggeredRef.current = true;
    if (coApplicants.length >= 8) {
      alert("You can add up to 8 inventors.");
      return;
    }
    const newCoApplicants = [...coApplicants, { name: '' }];
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);
    setIsModalOpen(true); // Open modal when a new additional inventor is added
  };

  const handleRemoveMember = (index) => {
    isSelfTriggeredRef.current = true;
    // We cannot drop below 3 elements in the visible state array
    let newCoApplicants = coApplicants.filter((_, idx) => idx !== index);
    while (newCoApplicants.length < 3) {
      newCoApplicants.push({ name: '' });
    }
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);
  };

  const handleMemberChange = (index, value) => {
    isSelfTriggeredRef.current = true;
    const newCoApplicants = coApplicants.map((member, idx) => {
      if (idx === index) {
        return { name: value };
      }
      return member;
    });
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);
  };

  return (
    <div className="card additional-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column', height: '100%', minHeight: '520px', maxHeight: '680px', overflow: 'hidden' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Sparkles className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Applicant Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
        {/* College Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-name" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px' }}>
            College Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="details-name"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '38px',
                paddingRight: '12px',
                height: '36px',
                fontSize: '13px',
                borderRadius: '6px'
              }}
              placeholder="Enter college name"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Principal Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-email" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px' }}>
            Principal Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="details-email"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '38px',
                paddingRight: '12px',
                height: '36px',
                fontSize: '13px',
                borderRadius: '6px'
              }}
              placeholder="Enter principal name"
              value={email}
              onChange={(e) => handleEmailChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Primary Inventors Header */}
        <div style={{
          marginTop: '6px',
          borderTop: '1px solid #F3F4F6',
          paddingTop: '12px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span style={{ fontSize: '13px', fontWeight: '600', color: '#1F2937', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Users size={16} style={{ color: '#0052cc' }} /> Primary Inventors
          </span>
        </div>

        {/* List of First 3 Inventors */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {coApplicants.slice(0, 3).map((member, index) => (
            <div key={index} className="form-group">
              <label className="form-label" htmlFor={`member-name-${index}`} style={{ fontSize: '11px', color: '#4B5563', fontWeight: '600' }}>
                Inventor #{index + 1} {index === 0 ? '*' : '(Optional)'}
              </label>
              <input
                id={`member-name-${index}`}
                type="text"
                className="login-input"
                style={{
                  background: '#F9FAFB',
                  border: '1px solid #D1D5DB',
                  color: '#1F2937',
                  paddingLeft: '12px',
                  paddingRight: '12px',
                  height: '36px',
                  fontSize: '13px',
                  borderRadius: '6px'
                }}
                placeholder={`Enter inventor #${index + 1} name`}
                value={member.name || ''}
                onChange={(e) => handleMemberChange(index, e.target.value)}
                required={index === 0}
              />
            </div>
          ))}
        </div>

        {/* Manage/Add buttons */}
        <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
          {coApplicants.length < 8 && (
            <button
              type="button"
              onClick={handleAddMember}
              style={{
                flex: 1,
                background: '#0052cc',
                color: '#FFF',
                border: 'none',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '4px',
                transition: 'background 0.2s'
              }}
            >
              <Plus size={14} /> Add Inventor
            </button>
          )}
          {coApplicants.length > 3 && (
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              style={{
                flex: 1,
                background: '#F3F4F6',
                color: '#1F2937',
                border: '1px solid #D1D5DB',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '4px',
                transition: 'background 0.2s'
              }}
            >
              Manage More ({coApplicants.length - 3})
            </button>
          )}
        </div>
      </div>

      {/* Additional Inventors Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          backgroundColor: 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
          animation: 'fadeIn 0.2s ease'
        }}>
          <div style={{
            background: '#FFF',
            borderRadius: '12px',
            width: '450px',
            maxWidth: '90%',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            border: '1px solid #E5E7EB',
            animation: 'slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px' }}>
              <span style={{ fontWeight: '600', fontSize: '1.05rem', color: '#1F2937', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Users size={18} style={{ color: '#0052cc' }} /> Additional Inventors
              </span>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#9CA3AF', padding: '4px', borderRadius: '50%' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '350px', overflowY: 'auto', paddingRight: '4px' }}>
              {coApplicants.slice(3).map((member, idx) => {
                const actualIndex = idx + 3;
                return (
                  <div key={actualIndex} style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                    padding: '12px',
                    backgroundColor: '#F9FAFB',
                    borderRadius: '8px',
                    border: '1px solid #E5E7EB'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontSize: '11px', fontWeight: '700', color: '#4B5563', textTransform: 'uppercase' }}>
                        Inventor #{actualIndex + 1}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveMember(actualIndex)}
                        style={{
                          background: 'transparent',
                          color: '#EF4444',
                          border: 'none',
                          fontSize: '11px',
                          fontWeight: '600',
                          cursor: 'pointer',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          transition: 'background 0.2s'
                        }}
                      >
                        Remove
                      </button>
                    </div>
                    <input
                      type="text"
                      className="login-input"
                      style={{
                        background: '#FFF',
                        border: '1px solid #D1D5DB',
                        color: '#1F2937',
                        paddingLeft: '12px',
                        paddingRight: '12px',
                        height: '36px',
                        fontSize: '13px',
                        borderRadius: '6px'
                      }}
                      placeholder={`Enter inventor #${actualIndex + 1} name`}
                      value={member.name || ''}
                      onChange={(e) => handleMemberChange(actualIndex, e.target.value)}
                      required
                    />
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', gap: '12px', borderTop: '1px solid #F3F4F6', paddingTop: '16px' }}>
              {coApplicants.length < 8 && (
                <button
                  type="button"
                  onClick={handleAddMember}
                  style={{
                    flex: 1,
                    background: '#F3F4F6',
                    color: '#1F2937',
                    border: '1px solid #D1D5DB',
                    padding: '10px',
                    borderRadius: '6px',
                    fontSize: '13px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    transition: 'background 0.2s'
                  }}
                >
                  + Add Another
                </button>
              )}
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                style={{
                  flex: 1,
                  background: '#0052cc',
                  color: '#FFF',
                  border: 'none',
                  padding: '10px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'background 0.2s'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdditionalDetailsCard;
