import { createContext, useContext, useState } from 'react'
import { X } from 'lucide-react'

const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const toast = (text, type = 'success') => {
    const id = Date.now()
    setItems((current) => [...current, { id, text, type }])
    setTimeout(() => setItems((current) => current.filter((item) => item.id !== id)), 4500)
  }
  return <ToastContext.Provider value={toast}>
    {children}
    <div className="toasts">{items.map((item) => <div className={`toast ${item.type}`} key={item.id}>
      <span>{item.text}</span><button onClick={() => setItems((current) => current.filter((x) => x.id !== item.id))}><X size={15} /></button>
    </div>)}</div>
  </ToastContext.Provider>
}

export const useToast = () => useContext(ToastContext)
