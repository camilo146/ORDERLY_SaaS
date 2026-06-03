import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { LUCIDE_ICONS, LucideIconProvider } from 'lucide-angular';
import {
  Package, LayoutDashboard, ShoppingCart, Headphones, Settings,
  Shield, LogOut, Plus, X, AlertTriangle, Check, CheckCircle,
  MessageCircle, QrCode, Building2, Users, TrendingUp, Clock,
  ChevronRight, ChevronLeft, RefreshCw, Utensils, Pill, Wrench,
  Shirt, Star, Zap, BarChart3, FileText, Store, Loader, Eye,
  EyeOff, ArrowRight, Circle, ToggleLeft, ToggleRight, Wifi, WifiOff,
  Map, Bot, CircleUser, Smartphone, Rocket, Package2, ClipboardList,
  Truck, AlertCircle, Pencil, Trash2, Image, Info, XCircle,
  Menu, ChevronDown, ArrowUpRight, Mail, Phone, MapPin, Globe, CreditCard, Play,
  DollarSign, Ban, UserCheck, UserX, Send, Activity, Filter, Search,
  Gift, ExternalLink, Crown, Database, Layers, ChevronUp, MoreHorizontal, Lock
} from 'lucide-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: LUCIDE_ICONS,
      multi: true,
      useValue: new LucideIconProvider({
        Package, LayoutDashboard, ShoppingCart, Headphones, Settings,
        Shield, LogOut, Plus, X, AlertTriangle, Check, CheckCircle,
        MessageCircle, QrCode, Building2, Users, TrendingUp, Clock,
        ChevronRight, ChevronLeft, RefreshCw, Utensils, Pill, Wrench,
        Shirt, Star, Zap, BarChart3, FileText, Store, Loader, Eye,
        EyeOff, ArrowRight, Circle, ToggleLeft, ToggleRight, Wifi, WifiOff,
        Map, Bot, CircleUser, Smartphone, Rocket, Package2, ClipboardList,
        Truck, AlertCircle, Pencil, Trash2, Image, Info, XCircle,
        Menu, ChevronDown, ArrowUpRight, Mail, Phone, MapPin, Globe, CreditCard, Play,
        DollarSign, Ban, UserCheck, UserX, Send, Activity, Filter, Search,
        Gift, ExternalLink, Crown, Database, Layers, ChevronUp, MoreHorizontal, Lock
      })
    }
  ]
};
