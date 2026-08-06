import React, { useState, useEffect, useRef } from 'react';
import { User, Sparkles } from 'lucide-react';

function AdditionalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [name, setName] = useState('');
  const [houseNo, setHouseNo] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [stateVal, setStateVal] = useState('');
  const [country, setCountry] = useState('');
  const [pincode, setPincode] = useState('');

  const isSelfTriggeredRef = useRef(false);

  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setName(user?.name || '');
    const addr = user?.address || {};
    setHouseNo(addr.houseNo || '');
    setStreet(addr.street || '');
    setCity(addr.city || '');
    setStateVal(addr.state || '');
    setCountry(addr.country || '');
    setPincode(addr.pincode || '');
  }, [user]);

  const syncChanges = (updatedName, updatedFields) => {
    isSelfTriggeredRef.current = true;

    const mergedAddress = {
      ...user?.address,
      houseNo: updatedFields.hasOwnProperty('houseNo') ? updatedFields.houseNo : houseNo,
      street: updatedFields.hasOwnProperty('street') ? updatedFields.street : street,
      city: updatedFields.hasOwnProperty('city') ? updatedFields.city : city,
      state: updatedFields.hasOwnProperty('state') ? updatedFields.state : stateVal,
      country: updatedFields.hasOwnProperty('country') ? updatedFields.country : country,
      pincode: updatedFields.hasOwnProperty('pincode') ? updatedFields.pincode : pincode,
    };

    const updatedUser = {
      ...user,
      name: updatedName,
      address: mergedAddress
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          name: updatedName,
          address: {
            ...previewData.applicant?.address,
            ...mergedAddress
          }
        }
      };
    } else {
      updatedData = {
        applicant: {
          name: updatedName,
          email: user?.email || '',
          address: mergedAddress
        },
        inventors: (user?.additionalMembers || []).map(m => ({
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
    setName(val);
    syncChanges(val, {});
  };

  const handleFieldChange = (fieldName, val) => {
    if (fieldName === 'houseNo') setHouseNo(val);
    else if (fieldName === 'street') setStreet(val);
    else if (fieldName === 'city') setCity(val);
    else if (fieldName === 'state') setStateVal(val);
    else if (fieldName === 'country') setCountry(val);
    else if (fieldName === 'pincode') setPincode(val);

    syncChanges(name, { [fieldName]: val });
  };

  const inputStyle = {
    background: '#F9FAFB',
    border: '1px solid #D1D5DB',
    color: '#1F2937',
    paddingLeft: '12px',
    paddingRight: '12px',
    height: '36px',
    fontSize: '13px',
    borderRadius: '6px',
    width: '100%'
  };

  const labelStyle = {
    color: '#4B5563',
    fontWeight: '600',
    fontSize: '12px',
    marginBottom: '4px',
    display: 'block'
  };

  return (
    <div className="card additional-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Sparkles className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Applicant Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {/* College Name */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="details-name">
            College Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="details-name"
              type="text"
              className="login-input"
              style={{
                ...inputStyle,
                paddingLeft: '38px'
              }}
              placeholder="Enter college name"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* House No */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-houseNo">House No.</label>
          <input
            id="addr-houseNo"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter house number"
            value={houseNo}
            onChange={(e) => handleFieldChange('houseNo', e.target.value)}
          />
        </div>

        {/* Street */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-street">Street</label>
          <input
            id="addr-street"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter street"
            value={street}
            onChange={(e) => handleFieldChange('street', e.target.value)}
          />
        </div>

        {/* City */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-city">City</label>
          <input
            id="addr-city"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter city"
            value={city}
            onChange={(e) => handleFieldChange('city', e.target.value)}
          />
        </div>

        {/* State */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-state">State</label>
          <input
            id="addr-state"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter state"
            value={stateVal}
            onChange={(e) => handleFieldChange('state', e.target.value)}
          />
        </div>

        {/* Country */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-country">Country</label>
          <input
            id="addr-country"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter country"
            value={country}
            onChange={(e) => handleFieldChange('country', e.target.value)}
          />
        </div>

        {/* Pin Code */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-pincode">Pincode</label>
          <input
            id="addr-pincode"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter pin code"
            value={pincode}
            onChange={(e) => handleFieldChange('pincode', e.target.value)}
          />
        </div>
      </div>
    </div>
  );
}

export default AdditionalDetailsCard;
